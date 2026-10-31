# ĐÁNH GIÁ NGHIỆP VỤ & RÀ SOÁT TOÀN DIỆN TASK 1 (DORMHUB BACKEND)

> **Tài liệu đối chiếu:** `DormHub_Business_Requirements_v1.1.md`  
> **Phạm vi Task 1:** Auth, Student Profile, Maintenance, Notification, Common Config/Security.  
> **Người thực hiện:** Task 1 Developer  
> **Ngày cập nhật:** 29/10/2026

---

## MỤC LỤC
1. [Rà soát chi tiết Entity & DTO từng Module](#1-rà-soát-chi-tiết-entity--dto-từng-module)
   - [1.1. Module Báo hỏng (Maintenance)](#11-module-báo-hỏng-maintenance)
   - [1.2. Module Hồ sơ sinh viên (Student Profile)](#12-module-hồ-sơ-sinh-viên-student-profile)
   - [1.3. Module Thông báo (Notification)](#13-module-thông-báo-notification)
   - [1.4. Module Xác thực & Phân quyền (Auth & Security)](#14-module-xác-thực--phân-quyền-auth--security)
2. [Làm rõ nghiệp vụ: Cơ chế tự động gán `roomId` sau khi Team 2 hoàn thành](#2-làm-rõ-nghiệp-vụ-cơ-chế-tự-động-gán-roomid-sau-khi-team-2-hoàn-thành)
3. [Tổng hợp Bugs & Vấn đề kỹ thuật cần sửa ngay](#3-tổng-hợp-bugs--vấn-đề-kỹ-thuật-cần-sửa-ngay)
4. [Lộ trình hành động chi tiết (Action Plan)](#4-lộ-trình-hành-động-chi-tiết-action-plan)

---

## 1. Rà soát chi tiết Entity & DTO từng Module

---

### 1.1. Module Báo hỏng (Maintenance)

#### A. Đánh giá hiện trạng
- Entity `MaintenanceRequest` hiện chỉ có: `id`, `title`, `description`, `maintenanceStatus`, `student`, `room`, `createdAt`, `updatedAt`, `resolvedAt`.
- DTO `MaintenanceCreateRequest` chỉ có: `title`, `description`, `roomId`.
- DTO `MaintenanceStatusUpdateRequest` chỉ có: `status`.
- DTO `MaintenanceResponse` chỉ trả: `id`, `title`, `description`, `status`, `studentId`, `roomId`, `createdAt`, `updatedAt`, `resolvedAt`.

#### B. Các thiếu sót lớn so với thực tế FE & Nghiệp vụ KTX:
1. **Thiếu Mức độ ưu tiên (`priority`):**
   - FE đã có field mức độ ưu tiên.
   - Nhân viên quản lý KTX bắt buộc phải phân loại độ khẩn cấp để xếp lịch thợ sửa chữa.
   - **Giải pháp:** Bổ sung Enum `MaintenancePriority`:
     - `LOW`: Thấp (đèn bàn mờ, hỏng móc treo đồ).
     - `MEDIUM`: Bình thường (quạt trần hơi kêu, khóa ban công rít).
     - `HIGH`: Cao (quạt hỏng giữa mùa nóng, rò rỉ nước lavabo).
     - `URGENT`: Khẩn cấp (chập điện bốc khói, vỡ ống nước chính gây ngập phòng).

2. **Thiếu Loại thiết bị / Sự cố (`type`):**
   - Báo hỏng cần phân nhóm thiết bị để giao đúng bộ phận (thợ điện lạnh, thợ mộc, thợ sửa ống nước).
   - **Giải pháp:** Bổ sung Enum `MaintenanceType`:
     - `FAN`: Quạt trần / quạt treo.
     - `AIR_CONDITIONER`: Máy lạnh / Điều hòa.
     - `REFRIGERATOR`: Tủ lạnh.
     - `LIGHTING`: Hệ thống đèn / bóng điện.
     - `PLUMBING`: Điện nước / Vòi sen / Bồn cầu / Ống thoát.
     - `FURNITURE`: Giường / Tủ quần áo / Bàn ghế học.
     - `DOOR_WINDOW`: Cửa chính / Cửa sổ / Khóa cửa.
     - `OTHER`: Khác.

3. **Thiếu Ảnh minh chứng sự cố (`imageUrl`):**
   - Sinh viên chụp ảnh thiết bị hư hỏng gửi lên (Cloudinary/S3 URL). Thợ kỹ thuật nhìn ảnh biết mang linh kiện gì theo để thay.
   - **Giải pháp:** Thêm field `imageUrl` (optional, length = 500) vào Entity và Request/Response.

4. **Thiếu Lý do từ chối / Ghi chú xử lý (`rejectionReason` / `note`):**
   - Khi Manager chuyển trạng thái sang `REJECTED`, bắt buộc phải có lý do (ví dụ: *"Thiết bị cá nhân của SV không thuộc danh mục KTX bảo trì"*, *"Báo nhầm số phòng"*).
   - Hiện tại `MaintenanceStatusUpdateRequest` chỉ có mỗi `status`, khiến sinh viên không hiểu tại sao bị từ chối.
   - **Giải pháp:** Bổ sung field `rejectionReason` (hoặc `note`) vào Entity và DTO.

5. **Thiếu thông tin hiển thị trực quan trong `MaintenanceResponse`:**
   - Hiện chỉ trả `roomId` và `studentId`. FE hiển thị danh sách hay chi tiết vé bảo trì cho Manager sẽ **không biết** đó là phòng số mấy (ví dụ: P.302, Tòa B) và sinh viên tên gì, số điện thoại nào nếu không gọi thêm API khác.
   - **Giải pháp:** Bổ sung vào `MaintenanceResponse`:
     - `roomNumber` (hoặc `roomName`), `buildingName`.
     - `studentName`, `studentCode`, `studentPhone` (rất quan trọng để thợ gọi điện hẹn giờ vào phòng sửa).

6. **Thiếu API tra cứu cho Manager:**
   - Hiện tại chỉ có `PATCH /api/v1/manager/maintenance/{id}/status`.
   - **Hoàn toàn thiếu:**
     - `GET /api/v1/manager/maintenance` (xem danh sách có phân trang `Pageable`, filter theo `status`, `type`, `priority`, `buildingId`).
     - `GET /api/v1/manager/maintenance/{id}` (xem chi tiết 1 vé báo hỏng).

---

### 1.2. Module Hồ sơ sinh viên (Student Profile)

#### A. Đánh giá hiện trạng
- Entity `Student`: có `studentCode`, `dateOfBirth`, `gender`, `address`, `avatarUrl`, `cic` (CCCD), `user`.
- Entity `User`: có `username`, `email`, `fullName`, `phone`, `role`, `status`.
- DTO `StudentProfileResponse`: có `studentId`, `studentCode`, `dateOfBirth`, `gender`, `address`, `avatarUrl`, `username`, `email`, `fullName`.
- DTO `StudentProfileUpdateRequest`: có `dateOfBirth`, `gender`, `address`, `avatarUrl`.

#### B. Các thiếu sót cần bổ sung:
1. **Thiếu Số điện thoại (`phone`) trong Response và Update:**
   - `phone` hiện lưu trong `User`, nhưng `StudentProfileResponse` không trả ra trường này! Sinh viên vào xem thông tin cá nhân sẽ không thấy SĐT của mình.
   - Trong `StudentProfileUpdateRequest`, sinh viên không thể đổi số điện thoại liên lạc. Trong đời sống sinh viên, việc đổi SĐT diễn ra thường xuyên.
   - **Giải pháp:** 
     - Thêm `phone` vào `StudentProfileResponse`.
     - Thêm `phone` vào `StudentProfileUpdateRequest` kèm validation `@Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$")`.
     - Trong `StudentService.updateProfile()`: cập nhật `student.getUser().setPhone(request.getPhone())`.

2. **Thiếu Căn cước công dân (`cic`) trong `StudentProfileResponse`:**
   - Entity `Student` đã có cột `cic`, nhưng Response lại bỏ sót trường này. KTX bắt buộc hiển thị số CCCD để sinh viên kiểm tra thông tin định danh nội trú.
   - **Giải pháp:** Thêm `cic` vào `StudentProfileResponse`.

---

### 1.3. Module Thông báo (Notification)

#### A. Đánh giá hiện trạng
- File [Notification.java](file:///e:/Ki_1_Nam_4/Backend_DormHub/src/main/java/com/dormhub/entity/Notification.java) và [NotificationReceiver.java](file:///e:/Ki_1_Nam_4/Backend_DormHub/src/main/java/com/dormhub/entity/NotificationReceiver.java) hiện tại **đang rỗng 0%** (chưa có `@Entity`, chưa có thuộc tính).
- Chưa có Repository, DTO, Service, Controller nào.

#### B. Thiết kế chuẩn theo BR Mục 17:
1. **Entity `Notification`:**
   - `id`: PK (Identity).
   - `type`: Enum `NotificationType` (`MAINTENANCE_STATUS_CHANGED`, `APPLICATION_APPROVED`, `INVOICE_CREATED`, `GENERAL`, v.v.).
   - `title`: `@Column(nullable = false, length = 200)`
   - `body`: `@Column(columnDefinition = "TEXT")`
   - `refId`: `@Column(name = "ref_id")` (ID của vé báo hỏng, hóa đơn, hoặc hợp đồng liên quan để khi click vào thông báo FE có thể chuyển hướng đúng trang).
   - `createdAt`: `@Column(name = "created_at", nullable = false, updatable = false)`

2. **Entity `NotificationReceiver`:**
   - `id`: PK.
   - `notification`: `@ManyToOne(fetch = FetchType.LAZY) Notification`
   - `user`: `@ManyToOne(fetch = FetchType.LAZY) User`
   - `isRead`: `boolean` (default: `false`)
   - `readAt`: `LocalDateTime` (ghi nhận thời điểm đọc)

3. **DTOs cần có:**
   - `NotificationResponse`: `id`, `type`, `title`, `body`, `refId`, `isRead`, `createdAt`, `readAt`.

4. **APIs cần xây dựng:**
   - `GET /api/v1/student/notifications`: Lấy danh sách thông báo của user đăng nhập (hỗ trợ phân trang `Pageable`, filter `unreadOnly`).
   - `GET /api/v1/student/notifications/unread-count`: Đếm số lượng thông báo chưa đọc (dùng cho icon quả chuông badge đỏ ở FE).
   - `PATCH /api/v1/student/notifications/{id}/read`: Đánh dấu 1 thông báo là đã đọc.
   - `PATCH /api/v1/student/notifications/read-all`: Đánh dấu tất cả thông báo là đã đọc.

5. **Shared Service cho toàn dự án:**
   - Viết method dùng chung trong `NotificationService`:
     ```java
     public void sendNotification(NotificationType type, String title, String body, Long refId, List<Long> receiverUserIds);
     ```
   - Tích hợp ngay vào `MaintenanceService.updateStatus()`: Mỗi khi Manager duyệt/chuyển trạng thái yêu cầu bảo trì, tự động bắn thông báo cho sinh viên đó.

---

### 1.4. Module Xác thực & Phân quyền (Auth & Security)

#### A. Đánh giá hiện trạng
- Đã tuân thủ rất tốt cấu trúc HttpOnly Cookie cho JWT và Refresh Token.
- Nghiệp vụ không cho public đăng ký (`POST /register` -> `denyAll()`) là hoàn toàn chính xác theo BR Mục 4.1.

#### B. Điểm cần hoàn thiện:
- Sửa 3 bug kỹ thuật đã chỉ ra ở Section 3.
- Bổ sung Scheduled Job dọn dẹp các `refresh_tokens` và `email_verifications` hết hạn.

---

## 2. Làm rõ nghiệp vụ: Cơ chế tự động gán `roomId` sau khi Team 2 hoàn thành

### Câu hỏi đặt ra:
> *"Khi các thành viên kia (Team 2/3) hoàn thành thì `roomId` sẽ không còn gửi từ FE mà tự áp vô đúng không?"*

### Trả lời & Khẳng định:
**CHÍNH XÁC 100%!**

#### 1. Lý do:
- **Bảo mật:** Không được tin tưởng `roomId` do client gửi lên. Nếu cho FE gửi `roomId`, sinh viên ở phòng 101 có thể sửa request gửi `roomId = 405` để báo hỏng giả mạo phòng khác.
- **Trải nghiệm người dùng:** Sinh viên chỉ cần bấm "Tạo báo hỏng", không cần bắt sinh viên phải chọn phòng họ đang ở vì hệ thống quản lý KTX phải tự biết sinh viên đó đang cư trú ở phòng nào.

#### 2. Luồng hoạt động chuẩn sau khi Team 2 (RoomAssignment) hoặc Team 3 (Contract) xong:
```
Client (Student) 
    │
    ▼ POST /api/v1/student/maintenance (Payload KHÔNG có roomId)
Backend MaintenanceService
    │
    ├─ 1. Lấy userId từ JWT AuthenticationPrincipal
    ├─ 2. Tìm Student từ userId
    ├─ 3. Query RoomAssignment đang ACTIVE của Student:
    │     RoomAssignment assignment = roomAssignmentRepository
    │         .findByStudentIdAndStatus(student.getId(), AssignmentStatus.ACTIVE)
    │         .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_IN_ANY_ROOM));
    │
    ├─ 4. Tự gán: Room currentRoom = assignment.getRoom();
    │     maintenance.setRoom(currentRoom);
    │
    └─ 5. Lưu vé báo hỏng & Bắn Notification
```

#### 3. Phương án chuyển tiếp trong thời gian chờ Team 2:
- Ở giai đoạn hiện tại (Team 2 chưa merge):
  - Giữ `roomId` trong `MaintenanceCreateRequest` nhưng có thể đánh dấu là tạm thời, hoặc cho phép `roomId` nullable nếu chưa có dữ liệu phòng.
  - Viết sẵn hàm placeholder trong `MaintenanceService`:
    ```java
    // TODO: Khi Team 2 merge, thay thế bằng roomAssignmentRepository.findActiveRoomByStudentId(student.getId())
    Room room = resolveStudentRoom(student, request.getRoomId());
    ```

---

## 3. Tổng hợp Bugs & Vấn đề kỹ thuật cần sửa ngay

| Mức độ | Vị trí | Hiện tượng | Cách khắc phục |
|---|---|---|---|
| 🔴 **BUG** | `AuthController.java` (dòng ~166) | Logout set `.secure(false)` thay vì dùng biến môi trường. Khi lên HTTPS production, cookie không được xóa. | Đổi thành `.secure(cookieSecure)` |
| 🟠 **BUG** | `AuthController.java` (dòng ~209) | Endpoint `/refresh` hardcode `.maxAge(Duration.ofMinutes(15))` | Đổi thành `.maxAge(Duration.ofMinutes(accessTokenMinutes))` |
| 🟠 **BUG** | `AuthService.java` (dòng ~299) | `resetPassword()` kiểm tra `verification.getAttempts() >= MAX_OTP_ATTEMPTS` gây NPE nếu `attempts` null. | Null-safe check: `int attempts = verification.getAttempts() == null ? 0 : verification.getAttempts();` |
| 🟡 **INCONSISTENCY** | `StudentController` & `MaintenanceController` | Các controller này trả về entity/DTO trần hoặc thiếu `ResponseEntity`, không đồng bộ với `ApiResponse<T>` của `AuthController`. | Bọc toàn bộ trả về dạng `ResponseEntity<ApiResponse<T>>`. |
| 🟡 **DB SCHEMA** | `MaintenanceRequest.java` | Field `title` và `description` không khai báo `@Column(nullable = false, length = ...)`. | Bổ sung constraints cho JPA/DDL. |

---

## 4. Lộ trình hành động chi tiết (Action Plan)

### Bước 1: Hoàn thiện Entity & DTO cho Maintenance (Làm ngay)
1. Tạo 2 Enum: `MaintenancePriority` và `MaintenanceType`.
2. Bổ sung vào `MaintenanceRequest.java`:
   - `priority`, `type`, `imageUrl`, `rejectionReason`.
   - Constraints `@Column` cho `title` và `description`.
3. Cập nhật `MaintenanceCreateRequest`: Thêm `priority`, `type`, `imageUrl` (optional).
4. Cập nhật `MaintenanceStatusUpdateRequest`: Thêm `rejectionReason` (optional).
5. Cập nhật `MaintenanceResponse`: Thêm `priority`, `type`, `imageUrl`, `rejectionReason`, `roomNumber`, `buildingName`, `studentName`, `studentPhone`.
6. Cập nhật `MaintenanceMapper` & `MaintenanceService`.

### Bước 2: Hoàn thiện Student Profile
1. Thêm `phone` và `cic` vào `StudentProfileResponse`.
2. Thêm `phone` vào `StudentProfileUpdateRequest` (kèm regex).
3. Cập nhật `StudentService.updateProfile()` để sync `phone` sang `User`.

### Bước 3: Sửa các lỗi kỹ thuật & Chuẩn hóa Response format
1. Fix 3 bug: logout secure, refresh maxAge, resetPassword NPE.
2. Chuẩn hóa tất cả endpoint trong `StudentController` và `MaintenanceController` về `ResponseEntity<ApiResponse<T>>`.

### Bước 4: Triển khai Module Notification (Trong lúc chờ Team 2/3/4)
1. Viết Entity `Notification` và `NotificationReceiver`.
2. Viết Repository cho cả 2 entity.
3. Viết `NotificationService` và method `sendNotification()`.
4. Viết `NotificationController` cho sinh viên (danh sách, đếm chưa đọc, đánh dấu đã đọc).
5. Kết nối thông báo tự động vào `MaintenanceService.updateStatus()`.

### Bước 5: Thêm Manager Endpoints cho Maintenance
1. Bổ sung `GET /api/v1/manager/maintenance` (phân trang + lọc theo status).
2. Bổ sung `GET /api/v1/manager/maintenance/{id}`.

### Bước 6: Tích hợp cuối cùng (Sau khi Team 2 bàn giao)
1. Xóa bỏ `roomId` khỏi `MaintenanceCreateRequest`.
2. Query phòng trực tiếp từ `RoomAssignment` của Team 2 trong `MaintenanceService.create()`.
