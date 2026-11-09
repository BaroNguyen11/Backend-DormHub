package com.dormhub.service;

import com.dormhub.entity.Room;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NGUỒN CHÂN LÝ DUY NHẤT cho trạng thái phòng (RoomStatus).
 *
 * Quy ước nghiệp vụ chuẩn (BR §6.3-6.4):
 * - occupancy (current_occupancy) = số chỗ đang được sử dụng thực tế
 *   (phân phòng ASSIGNED hoặc CHECKED_IN).
 * - hold = số hợp đồng PENDING đang giữ chỗ chờ thanh toán cọc.
 * - occupancy + holds <= capacity phải đúng ở mọi thời điểm.
 *
 * Trạng thái suy ra:
 * - MAINTENANCE  : trạng thái thủ công, không bị ghi đè (trừ khi override).
 * - OCCUPIED     : đủ người ở thật (occupancy >= capacity, không còn hold).
 * - RESERVED     : hết chỗ trống nhưng còn hold chờ cọc/nhận.
 * - AVAILABLE    : còn chỗ trống thật.
 *
 * Toàn bộ hệ thống KHÔNG được tự đặt trạng thái phòng bằng công thức riêng;
 * phải gọi service này để tránh lệch công thức (P0 thống nhất 1 hàm recompute).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomStatusService {

    private static final List<ContractStatus> HOLD_STATUSES = List.of(ContractStatus.PENDING);
    private static final List<RoomAssignmentStatus> OCCUPYING_STATUSES =
            List.of(RoomAssignmentStatus.ASSIGNED, RoomAssignmentStatus.CHECKED_IN);

    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;

    /**
     * Đếm lại occupancy từ RoomAssignment thực tế rồi tính lại trạng thái phòng.
     * Dùng cho các luồng nhận phòng / trả phòng / chuyển phòng.
     * Phòng đang MAINTENANCE giữ nguyên trạng thái.
     */
    @Transactional
    public void syncOccupancyAndRefreshStatus(Long roomId) {
        Room room = lockRoom(roomId);
        if (room.getStatus() != RoomStatus.MAINTENANCE) {
            int occupancy = Math.toIntExact(roomAssignmentRepository
                    .countByRoom_IdAndStatusIn(roomId, OCCUPYING_STATUSES));
            room.setCurrentOccupancy(occupancy);
        }
        applyComputedStatus(room, false);
        roomRepository.save(room);
    }

    /**
     * Chỉ tính lại trạng thái từ occupancy hiện có (không đếm lại assignment).
     * Dùng cho các luồng giữ/hủy giữ chỗ, tạo/hủy hợp đồng.
     */
    @Transactional
    public void refreshStatus(Long roomId) {
        Room room = lockRoom(roomId);
        applyComputedStatus(room, false);
        roomRepository.save(room);
    }

    /**
     * Tính lại trạng thái cho một Room đã được khóa (ForUpdate) trong giao dịch hiện tại.
     * @param overrideMaintenance true nếu được phép thoát khỏi MAINTENANCE
     *                            (ví dụ: MaintenanceService hoàn tất bảo trì).
     */
    public void refreshStatus(Room room, boolean overrideMaintenance) {
        applyComputedStatus(room, overrideMaintenance);
    }

    private void applyComputedStatus(Room room, boolean overrideMaintenance) {
        if (room.getCapacity() == null || room.getCapacity() <= 0) {
            return;
        }
        if (!overrideMaintenance && room.getStatus() == RoomStatus.MAINTENANCE) {
            return;
        }
        int occupancy = room.getCurrentOccupancy() == null ? 0 : room.getCurrentOccupancy();
        long holds = contractRepository.countByRoom_IdAndStatusIn(room.getId(), HOLD_STATUSES);
        if (occupancy < 0 || occupancy + holds > room.getCapacity()) {
            log.warn("Dữ liệu phòng #{} vượt sức chứa: occupancy={}, holds={}, capacity={}",
                    room.getId(), occupancy, holds, room.getCapacity());
        }
        if (occupancy + holds >= room.getCapacity()) {
            room.setStatus(holds > 0 ? RoomStatus.RESERVED : RoomStatus.OCCUPIED);
        } else {
            room.setStatus(RoomStatus.AVAILABLE);
        }
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }
}
