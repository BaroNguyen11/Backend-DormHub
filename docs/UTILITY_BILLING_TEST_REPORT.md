# DormHub V1 — Tự động phát hành hóa đơn điện nước

Ngày kiểm thử: 25/11/2026 (Asia/Ho_Chi_Minh).

## Triển khai

- BillingScheduler.generateUtilityInvoices: billing.utility.cron mặc định `0 0 3 * * *`, timezone Asia/Ho_Chi_Minh; profile scheduler-test đặt `-`. Không chạy utility qua startup reconciliation.

- Quét keyset MeterReading CONFIRMED có readingMonth trước tháng hiện tại và chưa có UTILITY. Không giới hạn tháng liền trước, nên hỗ trợ cung cấp dữ liệu muộn. Không sửa MeterReading.

- BillingBatchService cô lập từng lần gọi InvoiceService.createUtilityIfAbsent bằng transaction riêng qua Spring proxy. Một bản ghi sai không rollback hóa đơn phòng khác. Quét lỗi toàn cục được log và thoát; từng phòng lỗi được thử lại ở lượt sau.

- Tính tiền qua RoomMonthlyChargeCalculator hiện có, dùng electricityPrice/waterPrice snapshot từ MeterReading. Hai InvoiceDetail điện/nước; hạn 14 ngày từ issueDate.

- Chống trùng: PostgreSQL advisory lock utility riêng 824002; khóa Room trước check/create; unique uk_invoices_room_month là lớp bảo vệ DB. Manual create hiện có cũng khóa cùng Room.

- Invoice.contract nullable trong Entity/schema, nên hóa đơn UTILITY mới chỉ gắn Room, không chọn hợp đồng đại diện. Đường gọi Optional findActiveContractByRoomAndMonth cũ đã được bỏ khỏi InvoiceService; nhiều hợp đồng hoặc hợp đồng đã EXPIRED không gây lỗi chọn một kết quả.

- Quyền UTILITY của list/detail và payment cùng dùng điều kiện cư trú: check_in_at khác NULL, nhỏ hơn đầu tháng kế tiếp; check_out_at NULL hoặc lớn hơn đầu tháng tính tiền. Không phụ thuộc status ACTIVE hiện tại hay chủ hợp đồng đại diện. ROOM_FEE/DEPOSIT vẫn theo owner hợp đồng.

- Notification INVOICE_CREATED gửi một thông báo đến tập userId DISTINCT đã cư trú trong kỳ, gồm người đã trả phòng. Không gửi cho ASSIGNED chưa check-in hoặc phòng khác.

- Web chỉ thêm giải thích hóa đơn chung trên DetailScreen, kèm số phòng nếu có. Không có module/trang điện nước mới.

## File thay đổi

Backend Java: scheduler/BillingScheduler.java; service/BillingBatchService.java; service/InvoiceService.java; repository/MeterReadingRepository.java; repository/RoomAssignmentRepository.java; repository/InvoiceRepository.java; controller/StudentInvoiceController.java (chỉ sửa comment quyền truy cập).

Backend cấu hình/tài liệu: src/main/resources/application-scheduler-test.yml; docs/SCHEDULER_TESTING.md; báo cáo này.

Web: DormHub/src/components/Screen/DetailScreen.jsx.

Không đổi Entity, Enum, schema, migration, Java test hoặc Mobile. Không thao tác Git. Không cài dependency.

## Môi trường và thực thi

PostgreSQL 18.6 local; database `dormhub_scheduler_test`, owner postgres. Đã kiểm tra đây là database QA được tạo trong đợt Scheduler trước và chỉ thêm fixture có prefix `u617755` cùng một fixture concurrency. Không DROP/ghi đè dataset cũ; không ghi database phát triển `dormhub_test_seed`.

Backend chạy bằng JAR build từ source; profile scheduler-test được nạp từ JAR (không dùng file profile test bên ngoài). Bind 127.0.0.1:18081 và 18082 khi kiểm tra hai instance. Mail/AI trỏ localhost:9, SePay pull disabled, credential dummy; không chuyển tiền thật hoặc gọi webhook ngoài.

Tạo dữ liệu giả qua SQL trong database QA, gồm lịch sử cư trú cho kỳ trước, người đã trả phòng, người chưa Check-in, phòng khác, chỉ số sai/DRAFT và dữ liệu kỳ cũ. Không thay đồng hồ hệ điều hành.

Bật riêng utility với cron 10 giây, chờ lượt đầu rồi ít nhất hai lượt tiếp theo; SELECT sau transaction commit. Thanh toán dùng API cookie/CSRF thật + giao dịch SePay giả lập trong database QA và Scheduler đối soát hiện có. Không sửa logic thanh toán.

Build Backend `gradlew.bat build -x test`: PASS. Không chạy Java test trong dự án. Build Web: PASS; ESLint DetailScreen: PASS. Web build có cảnh báo bundle lớn. Chưa kiểm thử trực quan trên trình duyệt.

## Kết quả

Tổng 24 kiểm tra; PASS 24; FAIL 0; BLOCKED 0; NOT RUN 0.

| ID | Mục tiêu | Kết quả |

|---|---|---|

| U14 | Packaged profile tắt tất cả cron và startup | PASS |

| U01 | CONFIRMED hợp lệ; snapshot; detail và hạn 14 ngày | PASS |

| U02 | Chưa CONFIRMED | PASS |

| U03 | Chỉ số không hợp lệ | PASS |

| U04 | Kỳ chưa kết thúc | PASS |

| U-LIMIT | Không có lịch sử cư trú | PASS |

| U05 | Dữ liệu đến muộn 4 tháng | PASS |

| U06 | Ba lượt không tạo trùng | PASS |

| U07 | Phòng nhiều hợp đồng, không Optional nonunique | PASS |

| U-NOTIFY | Thông báo đúng cư trú, kể cả người đã trả phòng | PASS |

| U08-a | Quyền danh sách/chi tiết a | PASS |

| U08-b | Quyền danh sách/chi tiết b | PASS |

| U08-former | Quyền danh sách/chi tiết former | PASS |

| U08-assigned | Quyền danh sách/chi tiết assigned | PASS |

| U08-other | Quyền danh sách/chi tiết other | PASS |

| U13-ROOM_FEE | Không mở quyền ROOM_FEE cho bạn cùng phòng | PASS |

| U13-DEPOSIT | Không mở quyền DEPOSIT cho bạn cùng phòng | PASS |

| U-PAY-REUSE | Cùng phòng không tạo hai phiên thanh toán còn hạn | PASS |

| U12-DB | Đối soát giao dịch giả lập và không thanh toán trùng | PASS |

| U12-a | Thấy trạng thái PAID a | PASS |

| U12-b | Thấy trạng thái PAID b | PASS |

| U12-former | Thấy trạng thái PAID former | PASS |

| U-METER | Không chỉnh sửa MeterReading | PASS |

| U-CONCURRENT | Hai instance cùng lập hóa đơn, giữ unique và khóa phân tán | PASS |

## Chi tiết và SELECT trước/sau

### U14 — PASS

Packaged profile tắt tất cả cron và startup

Expected: Không invoice, không START, có startup disabled

Log: profile-default.log

Before:

```json

[]

```

Actual / after:

```json

[]

```

### U01 — PASS

CONFIRMED hợp lệ; snapshot; detail và hạn 14 ngày

Expected: Một invoice 100000, details=100000, term14days, không contract đại diện

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

[
  {
    "invoice_id": 43,
    "status": "UNPAID",
    "contract_id": null,
    "billing_month": "2026-10-01",
    "total_amount": 100000,
    "term": "14 days",
    "detail_total": 100000
  }
]

```

### U02 — PASS

Chưa CONFIRMED

Expected: Không phát hành, bản ghi lỗi không chặn phòng khác

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

[]

```

### U03 — PASS

Chỉ số không hợp lệ

Expected: Không phát hành, bản ghi lỗi không chặn phòng khác

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

[]

```

### U04 — PASS

Kỳ chưa kết thúc

Expected: Không phát hành, bản ghi lỗi không chặn phòng khác

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

[]

```

### U-LIMIT — PASS

Không có lịch sử cư trú

Expected: Không phát hành, bản ghi lỗi không chặn phòng khác

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

[]

```

### U05 — PASS

Dữ liệu đến muộn 4 tháng

Expected: Phát hành đúng kỳ cũ

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

[
  {
    "invoice_id": 44,
    "status": "UNPAID",
    "contract_id": null,
    "billing_month": "2026-07-01",
    "total_amount": 100000,
    "term": "14 days",
    "detail_total": 100000
  }
]

```

### U06 — PASS

Ba lượt không tạo trùng

Expected: Giữ nguyên invoice

Log: utility-three-ticks.log

Before:

```json

[
  {
    "invoice_id": 43,
    "status": "UNPAID",
    "contract_id": null,
    "billing_month": "2026-10-01",
    "total_amount": 100000,
    "term": "14 days",
    "detail_total": 100000
  }
]

```

Actual / after:

```json

[
  {
    "invoice_id": 43,
    "status": "UNPAID",
    "contract_id": null,
    "billing_month": "2026-10-01",
    "total_amount": 100000,
    "term": "14 days",
    "detail_total": 100000
  }
]

```

### U07 — PASS

Phòng nhiều hợp đồng, không Optional nonunique

Expected: Một hóa đơn chung không gắn hợp đồng

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

[
  {
    "invoice_id": 43,
    "status": "UNPAID",
    "contract_id": null,
    "billing_month": "2026-10-01",
    "total_amount": 100000,
    "term": "14 days",
    "detail_total": 100000
  }
]

```

### U-NOTIFY — PASS

Thông báo đúng cư trú, kể cả người đã trả phòng

Expected: Đúng a,b,former; không assigned/other

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

[
  {
    "id": 44,
    "type": "INVOICE_CREATED",
    "ref_id": 43,
    "user_id": 54
  },
  {
    "id": 44,
    "type": "INVOICE_CREATED",
    "ref_id": 43,
    "user_id": 55
  },
  {
    "id": 44,
    "type": "INVOICE_CREATED",
    "ref_id": 43,
    "user_id": 56
  }
]

```

### U08-a — PASS

Quyền danh sách/chi tiết a

Expected: Xem được

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

{
  "http": [
    200,
    200
  ],
  "visible": true,
  "invoiceStatus": "UNPAID"
}

```

### U08-b — PASS

Quyền danh sách/chi tiết b

Expected: Xem được

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

{
  "http": [
    200,
    200
  ],
  "visible": true,
  "invoiceStatus": "UNPAID"
}

```

### U08-former — PASS

Quyền danh sách/chi tiết former

Expected: Xem được

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

{
  "http": [
    200,
    200
  ],
  "visible": true,
  "invoiceStatus": "UNPAID"
}

```

### U08-assigned — PASS

Quyền danh sách/chi tiết assigned

Expected: Không xuất hiện; detail404

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

{
  "http": [
    200,
    404
  ],
  "visible": false,
  "invoiceStatus": null
}

```

### U08-other — PASS

Quyền danh sách/chi tiết other

Expected: Không xuất hiện; detail404

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

{
  "http": [
    200,
    404
  ],
  "visible": false,
  "invoiceStatus": null
}

```

### U13-ROOM_FEE — PASS

Không mở quyền ROOM_FEE cho bạn cùng phòng

Expected: Owner200, roommate404

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

{
  "owner": 200,
  "roommate": 404
}

```

### U13-DEPOSIT — PASS

Không mở quyền DEPOSIT cho bạn cùng phòng

Expected: Owner200, roommate404

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

{
  "owner": 200,
  "roommate": 404
}

```

### U-PAY-REUSE — PASS

Cùng phòng không tạo hai phiên thanh toán còn hạn

Expected: Cùng paymentId

Log: utility-three-ticks.log

Before:

```json

[]

```

Actual / after:

```json

{
  "http": [
    201,
    200
  ],
  "samePayment": true
}

```

### U12-DB — PASS

Đối soát giao dịch giả lập và không thanh toán trùng

Expected: Invoice PAID, Payment SUCCESS

Log: utility-fake-payment.log

Before:

```json

[
  {
    "invoice_id": 43,
    "status": "UNPAID",
    "contract_id": null,
    "billing_month": "2026-10-01",
    "total_amount": 100000,
    "term": "14 days",
    "detail_total": 100000
  }
]

```

Actual / after:

```json

{
  "invoices": [
    {
      "invoice_id": 43,
      "status": "PAID",
      "contract_id": null,
      "billing_month": "2026-10-01",
      "total_amount": 100000,
      "term": "14 days",
      "detail_total": 100000
    }
  ],
  "payment": [
    {
      "status": "SUCCESS",
      "paid_at": "2026-11-25T21:44:15.379298+07:00"
    }
  ],
  "transaction": [
    {
      "status": "MATCHED"
    }
  ]
}

```

### U12-a — PASS

Thấy trạng thái PAID a

Expected: 200 PAID

Log: utility-fake-payment.log

Before:

```json

[]

```

Actual / after:

```json

{
  "http": 200,
  "status": "PAID"
}

```

### U12-b — PASS

Thấy trạng thái PAID b

Expected: 200 PAID

Log: utility-fake-payment.log

Before:

```json

[]

```

Actual / after:

```json

{
  "http": 200,
  "status": "PAID"
}

```

### U12-former — PASS

Thấy trạng thái PAID former

Expected: 200 PAID

Log: utility-fake-payment.log

Before:

```json

[]

```

Actual / after:

```json

{
  "http": 200,
  "status": "PAID"
}

```

### U-METER — PASS

Không chỉnh sửa MeterReading

Expected: Không thay đổi

Log: utility-fake-payment.log

Before:

```json

[
  {
    "reading_id": 1,
    "created_at": "2026-11-25T21:42:53.244712",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-10-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 53
  },
  {
    "reading_id": 2,
    "created_at": "2026-11-25T21:42:53.408589",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-10-01",
    "status": "DRAFT",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 58
  },
  {
    "reading_id": 3,
    "created_at": "2026-11-25T21:42:53.567793",
    "electricity_new": 90.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-10-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 59
  },
  {
    "reading_id": 4,
    "created_at": "2026-11-25T21:42:53.726409",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-07-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 60
  },
  {
    "reading_id": 5,
    "created_at": "2026-11-25T21:42:53.887088",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-11-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 61
  },
  {
    "reading_id": 6,
    "created_at": "2026-11-25T21:42:54.096894",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-10-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 62
  }
]

```

Actual / after:

```json

[
  {
    "reading_id": 1,
    "created_at": "2026-11-25T21:42:53.244712",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-10-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 53
  },
  {
    "reading_id": 2,
    "created_at": "2026-11-25T21:42:53.408589",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-10-01",
    "status": "DRAFT",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 58
  },
  {
    "reading_id": 3,
    "created_at": "2026-11-25T21:42:53.567793",
    "electricity_new": 90.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-10-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 59
  },
  {
    "reading_id": 4,
    "created_at": "2026-11-25T21:42:53.726409",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-07-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 60
  },
  {
    "reading_id": 5,
    "created_at": "2026-11-25T21:42:53.887088",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-11-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 61
  },
  {
    "reading_id": 6,
    "created_at": "2026-11-25T21:42:54.096894",
    "electricity_new": 120.0,
    "electricity_old": 100.0,
    "electricity_price": 3500.0,
    "reading_month": "2026-10-01",
    "status": "CONFIRMED",
    "water_new": 12.0,
    "water_old": 10.0,
    "water_price": 15000.0,
    "room_id": 62
  }
]

```

### U-CONCURRENT — PASS

Hai instance cùng lập hóa đơn, giữ unique và khóa phân tán

Expected: Mỗi instance ít nhất 3 lượt, đúng 1 hóa đơn 18500

Log: utility-instance1.log + utility-instance2.log

Before:

```json

[]

```

Actual / after:

```json

{
  "invoices": [
    {
      "invoice_id": 47,
      "total_amount": 18500
    }
  ],
  "ticks": [
    5,
    4
  ]
}

```

## Log runtime (trích theo dòng)

### profile-default.log

```text

1418: 2026-11-25T21:43:06.922+07:00  INFO 19648 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

```

### utility-fake-payment.log

```text

1418: 2026-11-25T21:44:26.251+07:00  INFO 23000 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:44:30.030+07:00  INFO 23000 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions START at=2026-11-25T14:44:30.030461400Z

1423: 2026-11-25T21:44:30.346+07:00  INFO 23000 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Payment reconciliation candidates=2, matched=1

1424: 2026-11-25T21:44:30.347+07:00  INFO 23000 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions END at=2026-11-25T14:44:30.347431800Z

1425: 2026-11-25T21:44:40.007+07:00  INFO 23000 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions START at=2026-11-25T14:44:40.007371900Z

1427: 2026-11-25T21:44:40.018+07:00  INFO 23000 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Payment reconciliation candidates=1, matched=0

1428: 2026-11-25T21:44:40.018+07:00  INFO 23000 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions END at=2026-11-25T14:44:40.018900300Z

1429: 2026-11-25T21:44:50.012+07:00  INFO 23000 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions START at=2026-11-25T14:44:50.012327700Z

1431: 2026-11-25T21:44:50.019+07:00  INFO 23000 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Payment reconciliation candidates=1, matched=0

1432: 2026-11-25T21:44:50.020+07:00  INFO 23000 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions END at=2026-11-25T14:44:50.020692200Z

1443: 2026-11-25T21:44:51.548+07:00  INFO 23000 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed initialization in 121 ms

1448: 2026-11-25T21:44:52.142+07:00 DEBUG 23000 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1453: 2026-11-25T21:44:52.154+07:00 DEBUG 23000 --- [.1-18081-exec-3] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1460: 2026-11-25T21:44:52.484+07:00 DEBUG 23000 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1465: 2026-11-25T21:44:52.556+07:00 DEBUG 23000 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1470: 2026-11-25T21:44:52.671+07:00 DEBUG 23000 --- [.1-18081-exec-5] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1475: 2026-11-25T21:44:52.789+07:00 DEBUG 23000 --- [.1-18081-exec-6] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1480: 2026-11-25T21:44:52.804+07:00 DEBUG 23000 --- [.1-18081-exec-7] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1487: 2026-11-25T21:44:52.884+07:00 DEBUG 23000 --- [.1-18081-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1492: 2026-11-25T21:44:52.895+07:00 DEBUG 23000 --- [.1-18081-exec-9] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1497: 2026-11-25T21:44:52.915+07:00 DEBUG 23000 --- [1-18081-exec-10] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1502: 2026-11-25T21:44:53.067+07:00 DEBUG 23000 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1507: 2026-11-25T21:44:53.071+07:00 DEBUG 23000 --- [.1-18081-exec-3] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1514: 2026-11-25T21:44:53.179+07:00 DEBUG 23000 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1519: 2026-11-25T21:44:53.193+07:00 DEBUG 23000 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1524: 2026-11-25T21:44:53.214+07:00 DEBUG 23000 --- [.1-18081-exec-5] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

```

### utility-instance1.log

```text

1418: 2026-11-25T21:46:05.010+07:00  INFO 12316 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:46:10.012+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:46:10.012269100Z

1421: 2026-11-25T21:46:10.400+07:00 ERROR 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=3, roomId=59, month=2026-10, errorType=AppException

1422: 2026-11-25T21:46:10.427+07:00 ERROR 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=6, roomId=62, month=2026-10, errorType=AppException

1423: 2026-11-25T21:46:10.432+07:00  INFO 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing complete: created=0, skipped=0, failed=2

1424: 2026-11-25T21:46:10.433+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:46:10.433880400Z

1425: 2026-11-25T21:46:20.009+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:46:20.009239600Z

1426: 2026-11-25T21:46:20.029+07:00 ERROR 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=3, roomId=59, month=2026-10, errorType=AppException

1427: 2026-11-25T21:46:20.039+07:00 ERROR 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=6, roomId=62, month=2026-10, errorType=AppException

1428: 2026-11-25T21:46:20.139+07:00  INFO 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing complete: created=1, skipped=0, failed=2

1429: 2026-11-25T21:46:20.139+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:46:20.139959800Z

1430: 2026-11-25T21:46:29.999+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:46:29.999972100Z

1431: 2026-11-25T21:46:30.022+07:00 ERROR 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=3, roomId=59, month=2026-10, errorType=AppException

1432: 2026-11-25T21:46:30.037+07:00 ERROR 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=6, roomId=62, month=2026-10, errorType=AppException

1433: 2026-11-25T21:46:30.043+07:00  INFO 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing complete: created=0, skipped=0, failed=2

1434: 2026-11-25T21:46:30.043+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:46:30.043358100Z

1435: 2026-11-25T21:46:40.014+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:46:40.014694Z

1437: 2026-11-25T21:46:40.021+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : UTILITY billing skipped: another instance holds the lock

1438: 2026-11-25T21:46:40.021+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:46:40.021205700Z

1439: 2026-11-25T21:46:50.001+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:46:50.001219800Z

1440: 2026-11-25T21:46:50.018+07:00 ERROR 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=3, roomId=59, month=2026-10, errorType=AppException

1441: 2026-11-25T21:46:50.032+07:00 ERROR 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=6, roomId=62, month=2026-10, errorType=AppException

1442: 2026-11-25T21:46:50.039+07:00  INFO 12316 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing complete: created=0, skipped=0, failed=2

1443: 2026-11-25T21:46:50.039+07:00  INFO 12316 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:46:50.039945400Z

```

### utility-instance2.log

```text

1418: 2026-11-25T21:46:16.403+07:00  INFO 19864 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:46:20.018+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:46:20.018845200Z

1422: 2026-11-25T21:46:20.027+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : UTILITY billing skipped: another instance holds the lock

1423: 2026-11-25T21:46:20.027+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:46:20.027352Z

1424: 2026-11-25T21:46:30.015+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:46:30.015492500Z

1426: 2026-11-25T21:46:30.017+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : UTILITY billing skipped: another instance holds the lock

1427: 2026-11-25T21:46:30.017+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:46:30.017098700Z

1428: 2026-11-25T21:46:40.014+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:46:40.014694Z

1429: 2026-11-25T21:46:40.404+07:00 ERROR 19864 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=3, roomId=59, month=2026-10, errorType=AppException

1430: 2026-11-25T21:46:40.428+07:00 ERROR 19864 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=6, roomId=62, month=2026-10, errorType=AppException

1431: 2026-11-25T21:46:40.433+07:00  INFO 19864 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing complete: created=0, skipped=0, failed=2

1432: 2026-11-25T21:46:40.434+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:46:40.434799100Z

1433: 2026-11-25T21:46:50.001+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:46:50.001219800Z

1435: 2026-11-25T21:46:50.003+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : UTILITY billing skipped: another instance holds the lock

1436: 2026-11-25T21:46:50.003+07:00  INFO 19864 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:46:50.003220200Z

```

### utility-three-ticks.log

```text

1418: 2026-11-25T21:43:42.228+07:00  INFO 1236 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:43:50.009+07:00  INFO 1236 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:43:50.009329800Z

1421: 2026-11-25T21:43:50.581+07:00 ERROR 1236 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=3, roomId=59, month=2026-10, errorType=AppException

1422: 2026-11-25T21:43:50.627+07:00 ERROR 1236 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=6, roomId=62, month=2026-10, errorType=AppException

1423: 2026-11-25T21:43:50.632+07:00  INFO 1236 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing complete: created=2, skipped=0, failed=2

1424: 2026-11-25T21:43:50.632+07:00  INFO 1236 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:43:50.632875100Z

1425: 2026-11-25T21:44:00.006+07:00  INFO 1236 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:44:00.006821400Z

1426: 2026-11-25T21:44:00.039+07:00 ERROR 1236 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=3, roomId=59, month=2026-10, errorType=AppException

1427: 2026-11-25T21:44:00.060+07:00 ERROR 1236 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=6, roomId=62, month=2026-10, errorType=AppException

1428: 2026-11-25T21:44:00.064+07:00  INFO 1236 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing complete: created=0, skipped=0, failed=2

1429: 2026-11-25T21:44:00.065+07:00  INFO 1236 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:44:00.065213900Z

1430: 2026-11-25T21:44:10.001+07:00  INFO 1236 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices START at=2026-11-25T14:44:10.001685400Z

1431: 2026-11-25T21:44:10.016+07:00 ERROR 1236 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=3, roomId=59, month=2026-10, errorType=AppException

1432: 2026-11-25T21:44:10.026+07:00 ERROR 1236 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing failed: readingId=6, roomId=62, month=2026-10, errorType=AppException

1433: 2026-11-25T21:44:10.029+07:00  INFO 1236 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : UTILITY billing complete: created=0, skipped=0, failed=2

1434: 2026-11-25T21:44:10.029+07:00  INFO 1236 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateUtilityInvoices END at=2026-11-25T14:44:10.029590500Z

1445: 2026-11-25T21:44:11.672+07:00  INFO 1236 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 122 ms

1450: 2026-11-25T21:44:12.321+07:00 DEBUG 1236 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1455: 2026-11-25T21:44:12.330+07:00 DEBUG 1236 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1462: 2026-11-25T21:44:12.680+07:00 DEBUG 1236 --- [.1-18081-exec-5] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1467: 2026-11-25T21:44:12.749+07:00 DEBUG 1236 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1472: 2026-11-25T21:44:12.892+07:00 DEBUG 1236 --- [.1-18081-exec-3] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1477: 2026-11-25T21:44:12.958+07:00 DEBUG 1236 --- [.1-18081-exec-6] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1482: 2026-11-25T21:44:13.122+07:00 DEBUG 1236 --- [.1-18081-exec-7] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1487: 2026-11-25T21:44:13.136+07:00 DEBUG 1236 --- [.1-18081-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1494: 2026-11-25T21:44:13.216+07:00 DEBUG 1236 --- [.1-18081-exec-9] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1499: 2026-11-25T21:44:13.230+07:00 DEBUG 1236 --- [1-18081-exec-10] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1504: 2026-11-25T21:44:13.259+07:00 DEBUG 1236 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1509: 2026-11-25T21:44:13.297+07:00 DEBUG 1236 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1514: 2026-11-25T21:44:13.431+07:00 DEBUG 1236 --- [.1-18081-exec-5] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1519: 2026-11-25T21:44:13.436+07:00 DEBUG 1236 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1526: 2026-11-25T21:44:13.535+07:00 DEBUG 1236 --- [.1-18081-exec-3] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1531: 2026-11-25T21:44:13.560+07:00 DEBUG 1236 --- [.1-18081-exec-6] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1536: 2026-11-25T21:44:13.578+07:00 DEBUG 1236 --- [.1-18081-exec-7] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1541: 2026-11-25T21:44:13.616+07:00 DEBUG 1236 --- [.1-18081-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1546: 2026-11-25T21:44:13.773+07:00 DEBUG 1236 --- [.1-18081-exec-9] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1551: 2026-11-25T21:44:13.789+07:00 DEBUG 1236 --- [1-18081-exec-10] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1558: 2026-11-25T21:44:13.866+07:00 DEBUG 1236 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1563: 2026-11-25T21:44:13.877+07:00 DEBUG 1236 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1568: 2026-11-25T21:44:13.898+07:00 DEBUG 1236 --- [.1-18081-exec-5] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1575: 2026-11-25T21:44:13.920+07:00 DEBUG 1236 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 404 NOT_FOUND

1580: 2026-11-25T21:44:14.056+07:00 DEBUG 1236 --- [.1-18081-exec-3] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1585: 2026-11-25T21:44:14.085+07:00 DEBUG 1236 --- [.1-18081-exec-6] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1592: 2026-11-25T21:44:14.176+07:00 DEBUG 1236 --- [.1-18081-exec-7] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1597: 2026-11-25T21:44:14.187+07:00 DEBUG 1236 --- [.1-18081-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1602: 2026-11-25T21:44:14.222+07:00 DEBUG 1236 --- [.1-18081-exec-9] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1609: 2026-11-25T21:44:14.251+07:00 DEBUG 1236 --- [1-18081-exec-10] o.s.web.servlet.DispatcherServlet        : Completed 404 NOT_FOUND

1614: 2026-11-25T21:44:14.491+07:00 DEBUG 1236 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1621: 2026-11-25T21:44:14.520+07:00 DEBUG 1236 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 404 NOT_FOUND

1626: 2026-11-25T21:44:14.940+07:00 DEBUG 1236 --- [.1-18081-exec-5] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1633: 2026-11-25T21:44:14.961+07:00 DEBUG 1236 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 404 NOT_FOUND

1638: 2026-11-25T21:44:14.974+07:00 DEBUG 1236 --- [.1-18081-exec-3] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1643: 2026-11-25T21:44:15.055+07:00 DEBUG 1236 --- [.1-18081-exec-6] o.s.web.servlet.DispatcherServlet        : Completed 201 CREATED

1648: 2026-11-25T21:44:15.086+07:00 DEBUG 1236 --- [.1-18081-exec-7] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1653: 2026-11-25T21:44:15.106+07:00 DEBUG 1236 --- [.1-18081-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

```

## SELECT tái kiểm tra

```sql
SELECT i.invoice_id,i.room_id,i.contract_id,i.billing_month,i.status,
       i.total_amount,i.issue_date,i.due_date,SUM(d.amount) AS detail_total
FROM invoices i LEFT JOIN invoice_details d ON d.invoice_id=i.invoice_id
WHERE i.invoice_type='UTILITY'
GROUP BY i.invoice_id ORDER BY i.invoice_id;

SELECT room_id,billing_month,COUNT(*) FROM invoices
WHERE invoice_type='UTILITY' GROUP BY room_id,billing_month HAVING COUNT(*)>1;

SELECT n.id,n.ref_id,nr.user_id FROM notifications n
JOIN notification_receivers nr ON nr.notification_id=n.id
WHERE n.type='INVOICE_CREATED'
AND n.ref_id IN (SELECT invoice_id FROM invoices WHERE invoice_type='UTILITY')
ORDER BY n.id,nr.user_id;
```

## Giới hạn và lưu ý

- Nếu không có lịch sử cư trú giao với kỳ, không có đối tượng hợp lệ để xem/thanh toán. Backend báo lỗi nghiệp vụ cho phòng đó và thử lại ở lượt sau; không tự gắn hợp đồng không liên quan. Dữ liệu cư trú thiếu cần được hệ thống/quản lý cung cấp đúng.

- Hóa đơn mới không gắn Contract; hai Scheduler nhắc đến hạn/quá hạn hiện có chỉ xử lý hóa đơn có Contract nên chưa nhắc lại cho UTILITY dạng này. Thông báo phát hành gửi đầy đủ người đủ điều kiện. Không sửa các Scheduler nhắc khác theo giới hạn yêu cầu.

- Giá/chi tiết đã phát hành không tự cập nhật nếu nguồn ngoài thay MeterReading sau đó; job không chỉnh hoặc đồng bộ lại hóa đơn đã tồn tại.

- Không chia tiền, không thanh toán từng phần. Một người trả đủ thì hóa đơn PAID cho toàn phòng. Quyền lịch sử dựa trên RoomAssignment còn được giữ lại; dữ liệu lịch sử bị sửa/xóa sẽ ảnh hưởng quyền truy cập.

- Bản ghi invalid/no-residency được log failed và tiếp tục thử ở lượt sau. skipped chỉ đếm ứng viên đã được chọn nhưng có hóa đơn khi kiểm tra lại; hóa đơn đã tồn tại bị lọc từ truy vấn nên không nằm trong counter skipped.

- Không kiểm thử bank/email thật hoặc giao diện bằng trình duyệt; kiểm thử HTTP thực dùng CookieJar và lấy CSRF trước mutation.

- Script, fixture manifest, log, JSON evidence: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\utility-v1`. Database test được giữ để kiểm tra lại. Các tiến trình test đã được dừng khi script kết thúc.

## Fixture manifest

```json

{
  "tag": "u617755",
  "actors": {
    "a": {
      "user": 54,
      "student": 53,
      "room": 53,
      "contract": 53,
      "application": null
    },
    "b": {
      "user": 55,
      "student": 54,
      "room": 53,
      "contract": 54,
      "application": null
    },
    "former": {
      "user": 56,
      "student": 55,
      "room": 53,
      "contract": 55,
      "application": null
    },
    "assigned": {
      "user": 57,
      "student": 56,
      "room": 53,
      "contract": 56,
      "application": null
    },
    "other": {
      "user": 58,
      "student": 57,
      "room": 57,
      "contract": 57,
      "application": null
    }
  },
  "rooms": {
    "draft": {
      "user": 59,
      "student": 58,
      "room": 58,
      "contract": 58,
      "application": null
    },
    "bad": {
      "user": 60,
      "student": 59,
      "room": 59,
      "contract": 59,
      "application": null
    },
    "late": {
      "user": 61,
      "student": 60,
      "room": 60,
      "contract": 60,
      "application": null
    },
    "ongoing": {
      "user": 62,
      "student": 61,
      "room": 61,
      "contract": 61,
      "application": null
    },
    "empty": {
      "user": 63,
      "student": 62,
      "room": 62,
      "contract": 62,
      "application": null
    }
  },
  "meter_readings": [
    {
      "reading_id": 1,
      "created_at": "2026-11-25T21:42:53.244712",
      "electricity_new": 120.0,
      "electricity_old": 100.0,
      "electricity_price": 3500.0,
      "reading_month": "2026-10-01",
      "status": "CONFIRMED",
      "water_new": 12.0,
      "water_old": 10.0,
      "water_price": 15000.0,
      "room_id": 53
    },
    {
      "reading_id": 2,
      "created_at": "2026-11-25T21:42:53.408589",
      "electricity_new": 120.0,
      "electricity_old": 100.0,
      "electricity_price": 3500.0,
      "reading_month": "2026-10-01",
      "status": "DRAFT",
      "water_new": 12.0,
      "water_old": 10.0,
      "water_price": 15000.0,
      "room_id": 58
    },
    {
      "reading_id": 3,
      "created_at": "2026-11-25T21:42:53.567793",
      "electricity_new": 90.0,
      "electricity_old": 100.0,
      "electricity_price": 3500.0,
      "reading_month": "2026-10-01",
      "status": "CONFIRMED",
      "water_new": 12.0,
      "water_old": 10.0,
      "water_price": 15000.0,
      "room_id": 59
    },
    {
      "reading_id": 4,
      "created_at": "2026-11-25T21:42:53.726409",
      "electricity_new": 120.0,
      "electricity_old": 100.0,
      "electricity_price": 3500.0,
      "reading_month": "2026-07-01",
      "status": "CONFIRMED",
      "water_new": 12.0,
      "water_old": 10.0,
      "water_price": 15000.0,
      "room_id": 60
    },
    {
      "reading_id": 5,
      "created_at": "2026-11-25T21:42:53.887088",
      "electricity_new": 120.0,
      "electricity_old": 100.0,
      "electricity_price": 3500.0,
      "reading_month": "2026-11-01",
      "status": "CONFIRMED",
      "water_new": 12.0,
      "water_old": 10.0,
      "water_price": 15000.0,
      "room_id": 61
    },
    {
      "reading_id": 6,
      "created_at": "2026-11-25T21:42:54.096894",
      "electricity_new": 120.0,
      "electricity_old": 100.0,
      "electricity_price": 3500.0,
      "reading_month": "2026-10-01",
      "status": "CONFIRMED",
      "water_new": 12.0,
      "water_old": 10.0,
      "water_price": 15000.0,
      "room_id": 62
    }
  ]
}

```
## Bổ sung kiểm tra điện nước khi khởi động server

billing.utility.reconcile-on-startup mặc định true; profile scheduler-test false. Khi ACCEPTING_TRAFFIC lần đầu, dùng lại generateUtilityInvoices và toàn bộ khóa/chống trùng. Giữ lịch 03:00.

Build Backend build -x test: PASS. Runtime trên dormhub_scheduler_test, cron tắt toàn bộ:

US-OFF PASS

```json

{
  "id": "US-OFF",
  "goal": "Profile test tắt utility startup",
  "before": [],
  "after": [],
  "expected": "Không invoice, không utility START",
  "status": "PASS",
  "log": "startup-disabled.log"
}

```

US-ON-0 PASS

```json

{
  "id": "US-ON-0",
  "goal": "Startup phát hành / restart không trùng",
  "before": [],
  "after": [
    {
      "invoice_id": 48,
      "status": "UNPAID",
      "total_amount": 18500
    }
  ],
  "expected": "Một invoice 18500, đúng một lượt startup",
  "status": "PASS",
  "log": "startup-enabled-0.log"
}

```

US-ON-1 PASS

```json

{
  "id": "US-ON-1",
  "goal": "Startup phát hành / restart không trùng",
  "before": [
    {
      "invoice_id": 48,
      "status": "UNPAID",
      "total_amount": 18500
    }
  ],
  "after": [
    {
      "invoice_id": 48,
      "status": "UNPAID",
      "total_amount": 18500
    }
  ],
  "expected": "Một invoice 18500, đúng một lượt startup",
  "status": "PASS",
  "log": "startup-enabled-1.log"
}

```