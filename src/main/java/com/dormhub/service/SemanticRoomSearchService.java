package com.dormhub.service;

import com.dormhub.dto.request.SemanticRoomSearchRequest;
import com.dormhub.dto.response.RoomReindexResponse;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.dto.response.SemanticRoomSearchAnalysisResponse;
import com.dormhub.dto.response.SemanticRoomSearchResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Room;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AiSearchUnavailableException;
import com.dormhub.mapper.RoomMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.RoomVectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticRoomSearchService {
    private final RoomRepository roomRepository;
    private final RoomVectorRepository roomVectorRepository;
    private final ContractRepository contractRepository;
    private final RoomMapper roomMapper;
    private final RoomSearchStatisticsService statisticsService;

    /**
     * Tạo lại chỉ mục từ toàn bộ Room và Building hiện có, kể cả phòng đang bảo trì.
     * Mỗi Room có một document ID ổn định để lần chạy sau cập nhật thay vì tạo bản sao.
     * Sau khi ghi xong, xóa các vector không còn tương ứng với Room nào.
     */
    public RoomReindexResponse reindex() {
        try {
            List<Room> rooms = roomRepository.findAllWithBuilding();
            Set<String> validDocumentIds = new HashSet<>();
            for (Room room : rooms) {
                String documentId = documentId(room.getId());
                validDocumentIds.add(documentId);
                roomVectorRepository.upsert(Document.builder()
                        .id(documentId)
                        .text(describe(room))
                        .metadata(Map.of("roomId", room.getId().toString()))
                        .build());
            }

            // Chỉ xóa document vector mồ côi; không thay đổi bản ghi Room nghiệp vụ.
            List<String> orphanIds = roomVectorRepository.ids().stream()
                    .filter(id -> !validDocumentIds.contains(id))
                    .toList();
            roomVectorRepository.delete(orphanIds);
            return new RoomReindexResponse(rooms.size(), orphanIds.size());
        } catch (RuntimeException ex) {
            throw new AiSearchUnavailableException(ex);
        }
    }

    /**
     * Tìm các document gần nghĩa với câu truy vấn, rồi lấy Room thật để lọc chính xác.
     * Chỉ trả phòng AVAILABLE còn chỗ sau khi tính cả hợp đồng PENDING giữ chỗ.
     */
    @Transactional(readOnly = true)
    public List<SemanticRoomSearchResponse> search(SemanticRoomSearchRequest request) {
        try {
            int indexedCount = roomVectorRepository.count();
            if (indexedCount == 0) {
                return List.of();
            }
            // Yêu cầu tối đa số document đã index để bộ lọc nghiệp vụ không bỏ sót
            // ứng viên xếp sau; PgVectorStore vẫn áp dụng ngưỡng tương đồng mặc định.
            List<Document> matches = roomVectorRepository.search(request.getQuery().trim(), indexedCount);
            List<Long> roomIds = matches.stream()
                    .map(document -> roomId(document.getMetadata()))
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (roomIds.isEmpty()) {
                return List.of();
            }

            Map<Long, Room> roomsById = roomRepository.findByIdsWithBuilding(roomIds).stream()
                    .collect(Collectors.toMap(Room::getId, room -> room));
            Map<Long, Integer> holdsByRoom = contractRepository
                    .findRoomHoldCounts(roomIds, List.of(ContractStatus.PENDING)).stream()
                    .collect(Collectors.toMap(
                            ContractRepository.RoomHoldCountProjection::getRoomId,
                            projection -> Math.toIntExact(projection.getHoldCount())));

            List<SemanticRoomSearchResponse> results = new ArrayList<>();
            for (Document document : matches) {
                Long id = roomId(document.getMetadata());
                Room room = roomsById.get(id);
                if (room == null || !matchesExactFilters(room, request)) {
                    continue;
                }
                int held = holdsByRoom.getOrDefault(id, 0);
                boolean available = room.getStatus() == RoomStatus.AVAILABLE
                        && room.getCurrentOccupancy() + held < room.getCapacity();
                if (!available) {
                    continue;
                }
                RoomResponse response = roomMapper.toResponse(room);
                response.setIsAvailable(true);
                results.add(new SemanticRoomSearchResponse(response, document.getScore()));
                if (results.size() >= request.getLimit()) {
                    break;
                }
            }
            return results;
        } catch (RuntimeException ex) {
            throw new AiSearchUnavailableException(ex);
        }
    }

    /** Tìm một lần, lọc toàn bộ ứng viên theo dữ liệu nghiệp vụ rồi mới áp dụng limit. */
    @Transactional(readOnly = true)
    public SemanticRoomSearchAnalysisResponse analyze(SemanticRoomSearchRequest request) {
        try {
            int indexedCount = roomVectorRepository.count();
            if (indexedCount == 0) {
                return emptyAnalysis();
            }
            List<Document> matches = roomVectorRepository.search(request.getQuery().trim(), indexedCount);
            List<Long> roomIds = matches.stream()
                    .map(document -> roomId(document.getMetadata()))
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (roomIds.isEmpty()) {
                return emptyAnalysis();
            }

            Map<Long, Room> roomsById = roomRepository.findByIdsWithBuilding(roomIds).stream()
                    .collect(Collectors.toMap(Room::getId, room -> room));
            Map<Long, Integer> holdsByRoom = contractRepository
                    .findRoomHoldCounts(roomIds, List.of(ContractStatus.PENDING)).stream()
                    .collect(Collectors.toMap(
                            ContractRepository.RoomHoldCountProjection::getRoomId,
                            projection -> Math.toIntExact(projection.getHoldCount())));

            // LinkedHashMap giữ thứ tự điểm do VectorStore trả về và loại document trùng Room ID.
            Map<Long, Room> eligibleRooms = new LinkedHashMap<>();
            Map<Long, Double> scoresByRoom = new LinkedHashMap<>();
            for (Document document : matches) {
                Long id = roomId(document.getMetadata());
                if (id == null || eligibleRooms.containsKey(id)) {
                    continue;
                }
                Room room = roomsById.get(id);
                if (room == null || !matchesExactFilters(room, request)) {
                    continue;
                }
                int held = holdsByRoom.getOrDefault(id, 0);
                if (room.getStatus() != RoomStatus.AVAILABLE
                        || room.getCurrentOccupancy() + held >= room.getCapacity()) {
                    continue;
                }
                eligibleRooms.put(id, room);
                scoresByRoom.put(id, document.getScore());
            }

            List<SemanticRoomSearchResponse> displayed = eligibleRooms.entrySet().stream()
                    .limit(request.getLimit())
                    .map(entry -> {
                        RoomResponse response = roomMapper.toResponse(entry.getValue());
                        response.setIsAvailable(true);
                        return new SemanticRoomSearchResponse(response, scoresByRoom.get(entry.getKey()));
                    })
                    .toList();
            return new SemanticRoomSearchAnalysisResponse(
                    displayed,
                    eligibleRooms.size(),
                    displayed.size(),
                    statisticsService.calculate(new ArrayList<>(eligibleRooms.values())));
        } catch (RuntimeException ex) {
            throw new AiSearchUnavailableException(ex);
        }
    }

    private SemanticRoomSearchAnalysisResponse emptyAnalysis() {
        return new SemanticRoomSearchAnalysisResponse(
                List.of(), 0, 0, statisticsService.calculate(List.of()));
    }

    /** So sánh giá, tòa nhà và loại phòng trên entity Room thay vì suy đoán từ vector. */
    private boolean matchesExactFilters(Room room, SemanticRoomSearchRequest request) {
        return (request.getBuildingId() == null
                || request.getBuildingId().equals(room.getBuilding().getId()))
                && (request.getRoomType() == null || request.getRoomType() == room.getRoomType())
                && (request.getMaxPrice() == null
                || room.getPricePerMonth().compareTo(request.getMaxPrice()) <= 0);
    }

    /** Đọc roomId từ metadata; bỏ qua document có ID thiếu hoặc không hợp lệ. */
    private Long roomId(Map<String, Object> metadata) {
        Object value = metadata.get("roomId");
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Sinh cùng một UUID cho cùng Room ID để reindex có thể chạy lặp an toàn. */
    private String documentId(Long roomId ) {
        return UUID.nameUUIDFromBytes(("dormhub-room:" + roomId )
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Ghép các thuộc tính Room và Building thành văn bản tiếng Việt để tạo embedding. */
    private String describe(Room room ) {
        Building building = room.getBuilding();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Phòng", room.getRoomNumber());
        fields.put("Tòa nhà", building.getName());
        fields.put("Mã tòa", building.getBuildingCode());
        fields.put("Địa chỉ", building.getAddress());
        fields.put("Tầng", String.valueOf(room.getFloor()));
        fields.put("Loại phòng", switch (room.getRoomType()) {
            case SINGLE -> "phòng một người";
            case DOUBLE -> "phòng hai người";
            case QUAD -> "phòng bốn người";
            case SIX_BED -> "phòng sáu người";
            case EIGHT_BED -> "phòng tám người";
        });
        fields.put("Sức chứa", room.getCapacity() + " người");
        fields.put("Giá mỗi tháng", room.getPricePerMonth().toPlainString() + " đồng");
        fields.put("Mô tả phòng", room.getDescription());
        fields.put("Mô tả tòa", building.getDescription());
        return fields.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(". "));
    }
}
