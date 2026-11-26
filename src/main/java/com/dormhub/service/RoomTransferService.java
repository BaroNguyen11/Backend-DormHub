package com.dormhub.service;

import static com.dormhub.specification.SearchSpecifications.*;

import com.dormhub.dto.request.RoomTransferCreateRequest;
import com.dormhub.dto.request.RoomTransferReviewRequest;
import com.dormhub.dto.response.RoomTransferResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.RoomTransferRequest;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.TransferStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomTransferMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.RoomTransferRequestRepository;
import com.dormhub.repository.StudentRepository;
import com.dormhub.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomTransferService {

    private static final int MAX_PAGE_SIZE = 100;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final RoomAssignmentService residenceService;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<RoomAssignmentStatus> OCCUPYING_ASSIGNMENT_STATUSES =
            List.of(RoomAssignmentStatus.ASSIGNED, RoomAssignmentStatus.CHECKED_IN);

    private final RoomTransferRequestRepository transferRequestRepository;
    private final ContractRepository contractRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomTransferMapper roomTransferMapper;
    private final NotificationService notificationService;
    private final RoomStatusService roomStatusService;

    public RoomTransferService(RoomTransferRequestRepository transferRequestRepository,
                               ContractRepository contractRepository,
                               RoomAssignmentRepository roomAssignmentRepository,
                               RoomRepository roomRepository,
                               StudentRepository studentRepository,
                               UserRepository userRepository,
                               InvoiceRepository invoiceRepository,
                               RoomTransferMapper roomTransferMapper,
                               NotificationService notificationService,
                               RoomStatusService roomStatusService, RoomAssignmentService residenceService) {
        this.residenceService = residenceService;
        this.transferRequestRepository = transferRequestRepository;
        this.contractRepository = contractRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomTransferMapper = roomTransferMapper;
        this.notificationService = notificationService;
        this.roomStatusService = roomStatusService;
    }

    @Transactional
    public RoomTransferResponse create(RoomTransferCreateRequest request, Long userId) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        RoomAssignment currentAssignment = residenceService.lockResidenceChange(userId, false);
        Student student = currentAssignment.getStudent();
        residenceService.ensureNoOpenResidenceChange(student.getId(), null, null, null);

        Room preferredRoom = null;
        if (request.getPreferredRoomId() != null) {
            if (Objects.equals(request.getPreferredRoomId(), currentAssignment.getRoom().getId())) {
                throw new AppException(ErrorCode.TRANSFER_SAME_ROOM);
            }
            preferredRoom = roomRepository.findByIdForUpdate(request.getPreferredRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
            entityManager.refresh(preferredRoom);
        }

        if (preferredRoom == null) throw new AppException(ErrorCode.TRANSFER_ROOM_REQUIRED);
        validateDestinationRoom(student, preferredRoom, 0);
        ensureSamePrice(currentAssignment.getContract(), preferredRoom);

        RoomTransferRequest transferRequest = new RoomTransferRequest();
        transferRequest.setStudent(student);
        transferRequest.setCurrentAssignment(currentAssignment);
        transferRequest.setPreferredRoom(preferredRoom);
        transferRequest.setReason(trimToNull(request.getReason()));
        transferRequest.setStatus(TransferStatus.PENDING);
        transferRequest.setRequestedAt(LocalDateTime.now(BUSINESS_ZONE));

        RoomTransferRequest saved = transferRequestRepository.save(transferRequest);
        notificationService.notifyResidenceRequestManagers("Yêu cầu chuyển phòng mới",
                "Sinh viên " + student.getStudentCode() + " gửi yêu cầu chuyển phòng #" + saved.getTransferId() + ".");
        return roomTransferMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<RoomTransferResponse> getMyRequests(Long userId, int page, int size, TransferStatus status) {
        validatePage(page, size);
        Student student = getStudentByUserId(userId);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        return transferRequestRepository.findAll(
                com.dormhub.specification.SearchSpecifications.<RoomTransferRequest>equal("student.id", student.getId())
                        .and(equal("status", status)), pageable).map(roomTransferMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RoomTransferResponse getMyRequest(Long transferId, Long userId) {
        Student student = getStudentByUserId(userId);
        RoomTransferRequest transferRequest = transferRequestRepository
                .findByTransferIdAndStudent_Id(transferId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        return roomTransferMapper.toResponse(transferRequest);
    }

    @Transactional
    public RoomTransferResponse cancel(Long transferId, Long userId) {
        Student student = getStudentByUserId(userId);
        RoomTransferRequest transferRequest = getTransferRequestForUpdate(transferId);
        ensureStudentOwnsRequest(transferRequest, student.getId());
        ensurePending(transferRequest);

        transferRequest.setStatus(TransferStatus.CANCELLED);
        return roomTransferMapper.toResponse(transferRequest);
    }

    @Transactional(readOnly = true)
    public Page<RoomTransferResponse> getRequestsForManager(
            TransferStatus status, int page, int size, String search) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<RoomTransferRequest> requests = transferRequestRepository.findAll(
                com.dormhub.specification.SearchSpecifications.<RoomTransferRequest>text(search, "transferId", "student.studentCode", "student.fullName", "student.user.fullName", "currentAssignment.room.roomNumber", "preferredRoom.roomNumber", "approvedRoom.roomNumber", "reason", "reviewNote").and(equal("status", status)), pageable);
        return requests.map(roomTransferMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RoomTransferResponse getRequestForManager(Long transferId) {
        RoomTransferRequest transferRequest = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        return roomTransferMapper.toResponse(transferRequest);
    }

    @Transactional
    public RoomTransferResponse review(Long transferId, RoomTransferReviewRequest request,
                                       Long reviewerUserId) {
        if (request == null || request.getStatus() == null
                || (request.getStatus() != TransferStatus.APPROVED
                && request.getStatus() != TransferStatus.REJECTED)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Kết quả xử lý chỉ được là APPROVED hoặc REJECTED");
        }

        if (request.getStatus() == TransferStatus.REJECTED
                && (request.getReviewNote() == null || request.getReviewNote().isBlank()))
            throw new AppException(ErrorCode.INVALID_REQUEST, "Cần ghi rõ lý do từ chối chuyển phòng.");
        RoomTransferRequest transferRequest = getTransferRequestForUpdate(transferId);
        ensurePending(transferRequest);
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getStatus() == TransferStatus.APPROVED) {
            roomStatusService.ensureTransferHandoverReady();
            approveTransfer(transferRequest, request, reviewer);
        }

        transferRequest.setStatus(request.getStatus());
        transferRequest.setReviewedBy(reviewer);
        transferRequest.setReviewedAt(LocalDateTime.now(BUSINESS_ZONE));
        transferRequest.setReviewNote(trimToNull(request.getReviewNote()));
        RoomTransferRequest savedRequest = transferRequestRepository.saveAndFlush(transferRequest);
        if (savedRequest.getStatus() == TransferStatus.APPROVED)
            roomStatusService.refreshStatus(savedRequest.getApprovedRoom().getId());
        notifyStudent(savedRequest);
        return roomTransferMapper.toResponse(savedRequest);
    }

    @Transactional
    public RoomTransferResponse complete(Long transferId,
            com.dormhub.dto.request.RoomTransferCompleteRequest request, Long reviewerUserId) {
        roomStatusService.ensureTransferHandoverReady();
        RoomTransferRequest transfer = getTransferRequestForUpdate(transferId);
        if (transfer.getStatus() == TransferStatus.COMPLETED) return roomTransferMapper.toResponse(transfer);
        if (transfer.getStatus() != TransferStatus.APPROVED || transfer.getApprovedRoom() == null
                || transfer.getReviewedAt() == null) throw new AppException(ErrorCode.TRANSFER_INVALID_STATUS);
        if (request == null) throw new AppException(ErrorCode.INVALID_REQUEST);
        boolean overdue = LocalDateTime.now(BUSINESS_ZONE).isAfter(transfer.getReviewedAt().plusHours(72));
        if (overdue && (!Boolean.TRUE.equals(request.getLateCompletionConfirmed())
                || request.getNote() == null || request.getNote().isBlank())) {
            throw new AppException(ErrorCode.TRANSFER_INVALID_STATUS,
                    "Đã quá hạn 72 giờ. Cần xác minh bàn giao thực tế, xác nhận ngoại lệ và ghi rõ lý do.");
        }
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        RoomAssignment current = lockTransferStay(transfer, true);
        residenceService.ensureNoOpenResidenceChange(transfer.getStudent().getId(), null, transferId, null);
        residenceService.lockResidenceInvoiceRooms(transfer.getStudent().getId(), current.getRoom().getId(), transfer.getApprovedRoom().getId());
        Room destination = roomRepository.findByIdForUpdate(transfer.getApprovedRoom().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        entityManager.refresh(destination);
        if (Objects.equals(destination.getId(), current.getRoom().getId()))
            throw new AppException(ErrorCode.TRANSFER_SAME_ROOM);
        if (roomStatusService.transferHolds(destination.getId()) < 1)
            throw new AppException(ErrorCode.TRANSFER_ROOM_NOT_AVAILABLE, "Không tìm thấy suất giữ phù hợp cho yêu cầu.");
        validateDestinationRoom(transfer.getStudent(), destination, 1);
        ensureSamePrice(current.getContract(), destination);
        residenceService.ensureNoUnpaidResidenceInvoices(current.getContract());
        appendActionNote(transfer, reviewer, overdue ? "Hoàn tất ngoại lệ quá hạn" : "Hoàn tất bàn giao", request.getNote());
        moveAfterHandover(transfer, current, destination, reviewer);
        transfer.setStatus(TransferStatus.COMPLETED);
        transferRequestRepository.saveAndFlush(transfer);
        roomStatusService.syncOccupancyAndRefreshStatus(current.getRoom().getId());
        roomStatusService.syncOccupancyAndRefreshStatus(destination.getId());
        notifyStudent(transfer);
        return roomTransferMapper.toResponse(transfer);
    }

    @Transactional
    public RoomTransferResponse cancelApproved(Long transferId,
            com.dormhub.dto.request.RoomTransferCancelRequest request, Long reviewerUserId) {
        roomStatusService.ensureTransferHandoverReady();
        if (request == null || !Boolean.TRUE.equals(request.getStudentHasNotMoved())
                || request.getNote() == null || request.getNote().isBlank())
            throw new AppException(ErrorCode.INVALID_REQUEST, "Cần xác nhận chưa chuyển phòng thực tế và nhập lý do hủy.");
        RoomTransferRequest transfer = getTransferRequestForUpdate(transferId);
        if (transfer.getStatus() != TransferStatus.APPROVED || transfer.getApprovedRoom() == null)
            throw new AppException(ErrorCode.TRANSFER_INVALID_STATUS);
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        RoomAssignment current = lockTransferStay(transfer, false);
        residenceService.lockResidenceInvoiceRooms(transfer.getStudent().getId(), current.getRoom().getId(), transfer.getApprovedRoom().getId());
        appendActionNote(transfer, reviewer, "Hủy sau xác minh chưa bàn giao", request.getNote());
        transfer.setStatus(TransferStatus.CANCELLED);
        transferRequestRepository.saveAndFlush(transfer);
        roomStatusService.refreshStatus(transfer.getApprovedRoom().getId());
        notifyStudent(transfer);
        return roomTransferMapper.toResponse(transfer);
    }

    private RoomAssignment lockTransferStay(RoomTransferRequest transfer, boolean requireActive) {
        Contract contract = contractRepository.findByContractIdForUpdate(transfer.getCurrentAssignment().getContract().getContractId())
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        Student student = studentRepository.findByUserIdForUpdate(transfer.getStudent().getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        RoomAssignment current = roomAssignmentRepository.findByAssignmentIdForUpdate(transfer.getCurrentAssignment().getAssignmentId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
        entityManager.refresh(contract);
        entityManager.refresh(current);
        if (current.getStatus() != RoomAssignmentStatus.CHECKED_IN
                || !Objects.equals(current.getStudent().getId(), student.getId())
                || !Objects.equals(contract.getStudent().getId(), student.getId())
                || !Objects.equals(contract.getRoom().getId(), current.getRoom().getId()))
            throw new AppException(ErrorCode.TRANSFER_INVALID_STATUS, "Thông tin cư trú đã thay đổi. Cần kiểm tra lại bàn giao.");
        if (requireActive) ensureStudentIsCheckedIn(student, current);
        return current;
    }

    private void appendActionNote(RoomTransferRequest transfer, User reviewer, String action, String note) {
        String entry = action + " — Manager #" + reviewer.getId() + " — " + LocalDateTime.now(BUSINESS_ZONE)
                + (note == null || note.isBlank() ? "" : ": " + note.trim());
        String combined = (transfer.getReviewNote() == null ? "" : transfer.getReviewNote() + "\n") + entry;
        if (combined.length() > 1000) throw new AppException(ErrorCode.INVALID_REQUEST, "Tổng ghi chú xử lý tối đa 1000 ký tự.");
        transfer.setReviewNote(combined);
    }

    /**
     * Thứ tự khóa cố định của luồng duyệt chuyển phòng:
     * HỢP ĐỒNG → PHÂN PHÒNG → PHÒNG (khóa theo ID phòng tăng dần).
     * Khóa hợp đồng và phòng hiện tại TRƯỚC khi kiểm tra hóa đơn unpaid để
     * chặn TOCTOU: hóa đơn ROOM_FEE mới (tự khóa hợp đồng qua lockContract)
     * và hóa đơn UTILITY mới (tự khóa phòng qua lockRoom) không thể chen ngang
     * giữa lúc kiểm tra nợ và lúc chốt chuyển phòng.
     */
    private void approveTransfer(RoomTransferRequest transferRequest,
                                 RoomTransferReviewRequest request, User reviewer) {
        // (1) Khóa hợp đồng TRƯỚC: đường tạo hóa đơn ROOM_FEE cũng phải khóa
        // hợp đồng trước, nên không thể phát hành hóa đơn mới trong lúc duyệt.
        Long contractId = transferRequest.getCurrentAssignment().getContract().getContractId();
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        lockTransferStay(transferRequest, true);
        residenceService.ensureNoOpenResidenceChange(transferRequest.getStudent().getId(), null, transferRequest.getTransferId(), null);

        // (2) Đọc lại dữ liệu MỚI của phân phòng (khóa biên), không dùng snapshot
        // chụp lúc tạo request: nếu SV đã đổi phòng sau khi tạo request thì phân
        // phòng cũ sẽ không còn CHECKED_IN và bị từ chối ngay ở ensureStudentIsCheckedIn.
        Long currentAssignmentId = transferRequest.getCurrentAssignment().getAssignmentId();
        RoomAssignment currentAssignment = roomAssignmentRepository
                .findByAssignmentIdForUpdate(currentAssignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
        ensureStudentIsCheckedIn(transferRequest.getStudent(), currentAssignment);

        Long destinationRoomId = request.getApprovedRoomId() != null
                ? request.getApprovedRoomId()
                : transferRequest.getPreferredRoom() == null
                ? null : transferRequest.getPreferredRoom().getId();
        if (destinationRoomId == null || destinationRoomId <= 0) {
            throw new AppException(ErrorCode.TRANSFER_ROOM_REQUIRED);
        }
        // So sánh với phòng hiện tại theo dữ liệu MỚI vừa khóa (bước 2).
        if (Objects.equals(destinationRoomId, currentAssignment.getRoom().getId())) {
            throw new AppException(ErrorCode.TRANSFER_SAME_ROOM);
        }

        // (3) Khóa CẢ HAI phòng theo ID tăng dần để tránh deadlock khi hai yêu
        // cầu chuyển phòng ngược chiều (A: X→Y, B: Y→X) chạy đồng thời.
        residenceService.lockResidenceInvoiceRooms(transferRequest.getStudent().getId(), currentAssignment.getRoom().getId(), destinationRoomId);
        long currentRoomId = currentAssignment.getRoom().getId();
        long firstRoomId = Math.min(currentRoomId, destinationRoomId);
        long secondRoomId = Math.max(currentRoomId, destinationRoomId);
        Room firstRoom = roomRepository.findByIdForUpdate(firstRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Room secondRoom = secondRoomId == firstRoomId ? firstRoom
                : roomRepository.findByIdForUpdate(secondRoomId)
                        .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Room currentRoom = currentRoomId == firstRoomId ? firstRoom : secondRoom;
        Room destinationRoom = destinationRoomId == firstRoomId ? firstRoom : secondRoom;
        entityManager.refresh(firstRoom);
        if (secondRoom != firstRoom) entityManager.refresh(secondRoom);

        // (4) Kiểm tra nợ SAU KHI đã khóa hợp đồng + phòng hiện tại.
        ensureNoUnpaidInvoices(contract, currentRoom);

        validateDestinationRoom(transferRequest.getStudent(), destinationRoom, 0);

        // Quy tắc giá khi chuyển phòng (BR §13.4): chỉ cho chuyển sang phòng cùng mức giá
        // để tránh thay đổi monthlyRent/depositAmount âm thầm trong hợp đồng đang ACTIVE.
        ensureSamePrice(contract, destinationRoom);

        transferRequest.setApprovedRoom(destinationRoom);
    }

    private void moveAfterHandover(RoomTransferRequest transferRequest, RoomAssignment currentAssignment,
                                  Room destinationRoom, User reviewer) {
        Contract contract = currentAssignment.getContract();
        LocalDateTime transferTime = LocalDateTime.now(BUSINESS_ZONE);
        currentAssignment.setStatus(RoomAssignmentStatus.CHECKED_OUT);
        currentAssignment.setCheckOutAt(transferTime);
        roomAssignmentRepository.save(currentAssignment);

        RoomAssignment newAssignment = new RoomAssignment();
        newAssignment.setStudent(transferRequest.getStudent());
        newAssignment.setContract(currentAssignment.getContract());
        newAssignment.setRoom(destinationRoom);
        newAssignment.setAssignedBy(reviewer);
        newAssignment.setAssignedAt(transferTime);
        newAssignment.setCheckInAt(transferTime);
        newAssignment.setStatus(RoomAssignmentStatus.CHECKED_IN);
        roomAssignmentRepository.save(newAssignment);
        roomAssignmentRepository.flush();

        // Đồng bộ 2 phòng qua hàm recompute chuẩn duy nhất (phân biệt đúng RESERVED/OCCUPIED)


        contract.setRoom(destinationRoom);
        contractRepository.save(contract);
        Student student = transferRequest.getStudent();
        student.setRoom(destinationRoom);
        studentRepository.save(student);
        transferRequest.setApprovedRoom(destinationRoom);
    }

    /** Gọi SAU KHI đã khóa hợp đồng và phòng hiện tại — xem approveTransfer. */
    private void ensureNoUnpaidInvoices(Contract contract, Room currentRoom) {
        residenceService.ensureNoUnpaidResidenceInvoices(contract);
    }

    private void ensureSamePrice(Contract contract, Room room) {
        if (contract.getMonthlyRent() == null || room.getPricePerMonth() == null
                || room.getPricePerMonth().compareTo(contract.getMonthlyRent()) != 0)
            throw new AppException(ErrorCode.TRANSFER_PRICE_MISMATCH);
    }

    private void validateDestinationRoom(Student student, Room destinationRoom, int ownHold) {
        long assignmentOccupancy = roomAssignmentRepository.countByRoom_IdAndStatusIn(
                destinationRoom.getId(), OCCUPYING_ASSIGNMENT_STATUSES);
        int occupiedSlots = Math.max(destinationRoom.getCurrentOccupancy(),
                Math.toIntExact(assignmentOccupancy));
        occupiedSlots += Math.toIntExact(contractRepository.countByRoom_IdAndStatusIn(
                destinationRoom.getId(), List.of(ContractStatus.PENDING))
                + roomStatusService.pendingApplicationHolds(destinationRoom.getId())
                + roomStatusService.transferHolds(destinationRoom.getId())) - ownHold;
        boolean allowedStatus = destinationRoom.getStatus() == RoomStatus.AVAILABLE
                || (ownHold == 1 && destinationRoom.getStatus() == RoomStatus.RESERVED);
        if (!allowedStatus || destinationRoom.getCapacity() == null
                || occupiedSlots >= destinationRoom.getCapacity()) {
            throw new AppException(ErrorCode.TRANSFER_ROOM_NOT_AVAILABLE);
        }

        if (destinationRoom.getPricePerMonth() == null || destinationRoom.getPricePerMonth().signum() <= 0) {
            throw new AppException(ErrorCode.TRANSFER_ROOM_NOT_AVAILABLE, "Phòng chuyển đến chưa có giá hợp lệ");
        }

        Gender studentGender = student.getGender();
        Gender roomRestriction = destinationRoom.getBuilding().getGenderRestriction();
        if (roomRestriction == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        if (studentGender != Gender.MALE && studentGender != Gender.FEMALE) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }
        if (studentGender != roomRestriction) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
    }

    private void notifyStudent(RoomTransferRequest request) {
        User user = request.getStudent().getUser();
        if (user == null) return;
        String title;
        String body;
        switch (request.getStatus()) {
            case APPROVED -> {
                title = "Đã duyệt chuyển phòng, chờ bàn giao";
                body = "Bạn vẫn cư trú tại phòng " + request.getCurrentAssignment().getRoom().getRoomNumber()
                        + ". Phòng được duyệt: " + request.getApprovedRoom().getRoomNumber()
                        + ". Vui lòng liên hệ quản lý để bàn giao trong 72 giờ kể từ khi duyệt.";
            }
            case COMPLETED -> { title = "Chuyển phòng thành công"; body = "Đã xác nhận bàn giao và chuyển bạn sang phòng " + request.getApprovedRoom().getRoomNumber() + ". Giá thuê, tiền cọc và thời hạn hợp đồng giữ nguyên."; }
            case CANCELLED -> { title = "Đã hủy yêu cầu chuyển phòng"; body = "Yêu cầu chuyển phòng đã được hủy. Phòng cư trú của bạn không thay đổi."; }
            default -> { title = "Yêu cầu chuyển phòng đã bị từ chối"; body = "Yêu cầu chuyển phòng của bạn đã bị từ chối."; }
        }
        if (request.getReviewNote() != null) body += " Ghi chú: " + request.getReviewNote();
        notificationService.sendNotification(NotificationType.TRANSFER_PROCESSED, title, body, request.getTransferId(), user.getId());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }

    private void ensureStudentIsCheckedIn(Student student, RoomAssignment assignment) {
        if (!Objects.equals(assignment.getStudent().getId(), student.getId())) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND);
        }
        if (assignment.getStatus() != RoomAssignmentStatus.CHECKED_IN
                || assignment.getContract().getStatus() != ContractStatus.ACTIVE
                || assignment.getContract().getEndDate() == null
                || assignment.getContract().getEndDate().isBefore(LocalDateTime.now(BUSINESS_ZONE))) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS,
                    "Chỉ sinh viên đang CHECKED_IN theo hợp đồng ACTIVE mới được chuyển phòng");
        }
    }

    /** Khóa biên (PESSIMISTIC_WRITE) phân phòng CHECKED_IN mới nhất của SV. */
    private RoomAssignment getCurrentAssignmentForStudentForUpdate(Long studentId) {
        return roomAssignmentRepository
                .findAllByStudent_IdAndStatusOrderByAssignmentIdDescForUpdate(
                        studentId, RoomAssignmentStatus.CHECKED_IN)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
    }

    private Student getStudentByUserId(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    private RoomTransferRequest getTransferRequestForUpdate(Long transferId) {
        if (transferId == null || transferId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return transferRequestRepository.findByTransferIdForUpdate(transferId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
    }

    private void ensureStudentOwnsRequest(RoomTransferRequest request, Long studentId) {
        if (!Objects.equals(request.getStudent().getId(), studentId)) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND);
        }
    }

    private void ensurePending(RoomTransferRequest request) {
        if (request.getStatus() != TransferStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSFER_INVALID_STATUS);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
