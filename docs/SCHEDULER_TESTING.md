# Kiểm thử Scheduler độc lập

Profile `scheduler-test` tắt cả 10 cron và billing khi ACCEPTING_TRAFFIC.
Profile không tự thay datasource, không đổi các hạn nghiệp vụ và không tạo dữ liệu.
Chỉ bật profile cùng database test riêng đã được xác minh. Không dùng database vận hành.

## Khởi động trên Windows PowerShell

Từ thư mục Backend-DormHub, sau khi cấu hình datasource test và các biến môi trường
theo cơ chế hiện có của dự án:

```powershell
rtk proxy .\gradlew.bat bootRun '--args=--spring.profiles.active=scheduler-test'
```

Nếu có profile local riêng cần kết hợp, dùng `local,scheduler-test`; đặt scheduler-test
sau cùng. Kiểm tra các environment variable/command-line override vì chúng có thể bật
job dù profile đã đặt cron `-`.

Bật đúng một job bằng command-line (thay property theo bảng bên dưới):

```powershell
rtk proxy .\gradlew.bat bootRun '--args=--spring.profiles.active=scheduler-test --dormhub.scheduler.contract.unpaid-deposits-cron="*/10 * * * * *"'
```

Cron chạy tại giây 00, 10, 20, 30, 40, 50. Chu kỳ không rút ngắn điều kiện dữ liệu.
Dừng bằng Ctrl+C trước khi chuyển job; bỏ override để trở về mặc định tắt.
Không cần sửa source giữa các lần chạy. Có thể truyền `--<property>=-` để tắt rõ ràng.

| Method | Property cron | Mặc định ngoài profile test |
|---|---|---|
| ContractLifecycleScheduler.expireUnpaidDeposits | dormhub.scheduler.contract.unpaid-deposits-cron | 0 */10 * * * * |
| ContractLifecycleScheduler.expireOverdueContracts | dormhub.scheduler.contract.expire-cron | 0 0 1 * * * |
| ContractLifecycleScheduler.remindExpiringContracts | dormhub.scheduler.contract.reminder-cron | 0 0 9 * * * |
| BillingScheduler.generateMonthlyRoomFees | billing.room-fee.cron | 0 0 1 1 * * |
| BillingScheduler.reconcileDailyRoomFees | billing.room-fee.reconcile-cron | 0 0 2 * * * |
| PaymentReconciliationScheduler.retryUnmatchedTransactions | dormhub.payment.reconcile-cron | 0 */10 * * * * |
| NotificationScheduler.sendInvoiceDueReminders | dormhub.scheduler.notification.due-reminder-cron | 0 0 9 * * * |
| NotificationScheduler.sendOverdueInvoiceReminders | dormhub.scheduler.notification.overdue-reminder-cron | 0 30 9 * * * |
| NotificationScheduler.cleanupOldNotifications | dormhub.scheduler.notification.cleanup-cron | 0 15 3 * * * |
| TokenCleanupTask.cleanupExpiredTokens | dormhub.scheduler.token.cleanup-cron | 0 0 2 * * ? |

`billing.room-fee.reconcile-on-startup`: mặc định `true`, profile test đặt `false`.
Để kiểm thử riêng event ACCEPTING_TRAFFIC, giữ mọi cron `-` và truyền
`--billing.room-fee.reconcile-on-startup=true`. Mỗi lần khởi động có thể lập bù nhiều
tháng; chỉ thực hiện trên database test. Công tắc không ngăn các cron billing đã bật.

Timezone giữ nguyên: Asia/Ho_Chi_Minh cho billing, contract và notification;
payment reconciliation và token cleanup dùng timezone mặc định hiện tại.

## Quy trình và dữ liệu

1. Đề xuất tạo database test riêng và user riêng chỉ có quyền trên database đó.
   Chưa tạo database/user hoặc chỉnh dữ liệu trong đợt triển khai này.
2. Dùng schema tương thích từ quy trình cấp database hiện có; giữ ddl-auto=validate.
   Nếu sao chép dữ liệu, cần bản đã ẩn danh và không chứa credential/giao dịch thật.
3. Tạo dữ liệu mới qua API nghiệp vụ trên môi trường test sau khi được phê duyệt.
   Không dùng tài khoản ngân hàng, webhook hoặc email vận hành để mô phỏng thanh toán.
4. Chạy SELECT trước; lưu ID, trạng thái, số lượng. Bật một job, chờ hai lượt,
   chạy lại SELECT trong phiên truy vấn mới để so sánh. Dừng Backend khi xong.
5. Không tạo API thủ công, không đổi đồng hồ hệ thống hoặc timestamp nghiệp vụ.

| Job | Dữ liệu đủ điều kiện và đối chứng |
|---|---|
| Hủy cọc | PENDING/UNPAID, dueDate quá hơn 1 giờ (hạn gốc duyệt +48 giờ); đối chứng có Payment SUCCESS, PENDING còn hạn, SePay UNMATCHED/REVIEW_REQUIRED |
| Hết hạn hợp đồng | ACTIVE có endDate trong quá khứ; đối chứng ACTIVE tương lai, PENDING |
| Nhắc hợp đồng | ACTIVE hết hạn trong 7 ngày, có Student/User; đối chứng ngoài khoảng và đã có thông báo |
| Hai lịch billing | ACTIVE bắt đầu không sau hôm nay, kỳ còn thiếu; đối chứng PENDING, startDate tương lai và kỳ đã có hóa đơn |
| Đối soát | SePay UNMATCHED nhận trong 24 giờ, Payment phù hợp; đối chứng sai số tiền/hạn, MATCHED, REVIEW_REQUIRED, nhận quá 24 giờ |
| Nhắc đến hạn | UNPAID đến hạn từ đầu hôm nay đến cuối ngày +3, có Contract/Student/User; đối chứng PAID và ngày +4 |
| Nhắc quá hạn | UNPAID đến hạn từ khoảng hai năm trước đến cuối hôm qua; đối chứng hôm nay/PAID |
| Dọn thông báo | CreatedAt trước 90 ngày; đối chứng mới, cả đã đọc và chưa đọc |
| Dọn token | Refresh token revoked/hết hạn; OTP used/hết hạn; đối chứng còn hiệu lực |

Không có Scheduler tự tịch thu cọc/hủy lịch sau 7 ngày. Không có cơ chế Clock có thể
điều khiển trong thay đổi này. Nếu chưa có dữ liệu đủ tuổi, đánh dấu BLOCKED cho ca
thời gian tương ứng; cron 10 giây chỉ giúp chạy tác vụ thường xuyên hơn.

## Log

Các job ghi START/END cùng tên và Instant UTC. END nghĩa là đã thoát phần xử lý,
không đảm bảo transaction commit thành công. Lỗi không bị nuốt thêm: lỗi chưa bắt
tiếp tục được cơ chế Scheduler của Spring ghi nhận; các lỗi theo bản ghi giữ log cũ.
BillingBatchService giữ thống kê created/skipped/failed theo tháng.
Deposit expiry thống kê candidates/evaluated/failed: evaluated KHÔNG có nghĩa đã hủy
vì service có thể bỏ qua. Dùng SELECT xác minh trạng thái cuối.

## SELECT trước và sau (không chứa hash/token/OTP)

Chỉ chạy trên database test; có thể thêm điều kiện ID test để thu hẹp kết quả.
Không giữ transaction snapshot cũ giữa hai lần đối chiếu.

### Hủy cọc quá hạn

```sql
SELECT i.invoice_id, i.due_date, i.status AS invoice_status,
       c.contract_id, c.status AS contract_status, c.deposit_status,
       d.id AS application_id, d.status AS application_status,
       r.id AS room_id, r.status AS room_status, r.current_occupancy
FROM invoices i JOIN contracts c ON c.contract_id = i.contract_id
LEFT JOIN dorm_applications d ON d.id = c.dorm_application_id
JOIN rooms r ON r.id = c.room_id
WHERE i.invoice_type = 'DEPOSIT'
ORDER BY i.invoice_id;
```

Mong đợi ứng viên hợp lệ: invoice/contract/application CANCELLED, room status tính lại.
Không xóa Payment SUCCESS, không giảm occupancy thêm lần nữa khi chạy lại.

### Hết hạn hợp đồng

```sql
SELECT c.contract_id, c.status, c.end_date, c.updated_at,
       a.assignment_id, a.status AS assignment_status, r.current_occupancy
FROM contracts c
LEFT JOIN room_assignments a ON a.contract_id = c.contract_id
JOIN rooms r ON r.id = c.room_id
WHERE c.end_date < CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh'
ORDER BY c.contract_id;
```

Mong đợi ACTIVE -> EXPIRED. Job hiện tại chỉ cập nhật contracts.status,
không tự checkout hoặc giải phóng occupancy.

### Hai lịch billing và lập bù lúc khởi động

```sql
SELECT i.contract_id, i.invoice_id, i.billing_month, i.status,
       i.total_amount, COUNT(d.detail_id) AS detail_count,
       COALESCE(SUM(d.amount), 0) AS detail_total
FROM invoices i LEFT JOIN invoice_details d ON d.invoice_id = i.invoice_id
WHERE i.invoice_type = 'ROOM_FEE'
GROUP BY i.contract_id, i.invoice_id, i.billing_month, i.status, i.total_amount
ORDER BY i.contract_id, i.billing_month;

SELECT contract_id, billing_month, COUNT(*) AS duplicate_count
FROM invoices WHERE invoice_type = 'ROOM_FEE'
GROUP BY contract_id, billing_month HAVING COUNT(*) > 1;
```

Mong đợi tạo kỳ thiếu; tổng chi tiết khớp hóa đơn; không thêm trùng ở lượt thứ hai.
Tháng đầu tính ngày thực tế HALF_UP; tháng sau theo monthlyRent.

### Đối soát

```sql
SELECT t.id, t.status AS transaction_status, t.received_at, t.transaction_at,
       t.review_reason, t.payment_id, p.status AS payment_status,
       p.paid_at, p.expires_at, i.invoice_id, i.status AS invoice_status,
       c.contract_id, c.status AS contract_status, c.deposit_status,
       a.assignment_id, a.status AS assignment_status, r.current_occupancy
FROM sepay_transactions t
LEFT JOIN payments p ON p.payment_id = t.payment_id
LEFT JOIN invoices i ON i.invoice_id = p.invoice_id
LEFT JOIN contracts c ON c.contract_id = i.contract_id
LEFT JOIN room_assignments a ON a.contract_id = c.contract_id
LEFT JOIN rooms r ON r.id = c.room_id
ORDER BY t.id;
```

Mong đợi khi khớp: MATCHED, SUCCESS/paidAt, invoice PAID. Với cọc: HELD,
ASSIGNED và occupancy chỉ tăng một lần. Job không tải giao dịch mới từ SePay.

### Ba job nhắc (hợp đồng, hóa đơn đến hạn, hóa đơn quá hạn)

```sql
SELECT n.id, n.type, n.ref_id, n.created_at, COUNT(nr.id) AS receiver_count
FROM notifications n
LEFT JOIN notification_receivers nr ON nr.notification_id = n.id
WHERE n.type IN ('CONTRACT_EXPIRING','INVOICE_DUE_REMINDER','INVOICE_OVERDUE')
GROUP BY n.id, n.type, n.ref_id, n.created_at ORDER BY n.id;

SELECT type, ref_id, COUNT(*) AS duplicate_count
FROM notifications
WHERE type IN ('CONTRACT_EXPIRING','INVOICE_DUE_REMINDER','INVOICE_OVERDUE')
GROUP BY type, ref_id HAVING COUNT(*) > 1;
```

Mong đợi đúng loại/refId/người nhận, không tăng ở lượt thứ hai. Chưa có khóa phân tán
cho các job nhắc; kiểm thử nhiều instance là ca riêng, không bật mặc định.

### Dọn thông báo

```sql
SELECT COUNT(*) AS old_notifications FROM notifications
WHERE created_at < (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh') - INTERVAL '90 days';
SELECT COUNT(*) AS old_receivers FROM notification_receivers nr
JOIN notifications n ON n.id = nr.notification_id
WHERE n.created_at < (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh') - INTERVAL '90 days';
```

Mong đợi cả hai về 0; dữ liệu mới giữ nguyên. Dữ liệu cũ chưa đọc cũng bị dọn theo logic cũ.

### Dọn token/OTP

Job dùng timezone JVM. Chỉ dùng LOCALTIMESTAMP khi timezone phiên PostgreSQL
khớp JVM; nếu không, thay mốc bằng local timestamp tương ứng timezone JVM.

```sql
SELECT COUNT(*) AS eligible_refresh_tokens FROM refresh_tokens
WHERE revoked_at IS NOT NULL OR expires_at < LOCALTIMESTAMP;
SELECT COUNT(*) AS eligible_otps FROM email_verifications
WHERE used_at IS NOT NULL OR expires_at < LOCALTIMESTAMP;
SELECT COUNT(*) AS valid_refresh_tokens FROM refresh_tokens
WHERE revoked_at IS NULL AND expires_at >= LOCALTIMESTAMP;
SELECT COUNT(*) AS valid_otps FROM email_verifications
WHERE used_at IS NULL AND expires_at >= LOCALTIMESTAMP;
```

Mong đợi nhóm đủ điều kiện về 0; nhóm còn hiệu lực không bị xóa (tính đến thời gian trôi qua).
