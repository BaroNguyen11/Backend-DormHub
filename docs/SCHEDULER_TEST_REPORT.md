# Báo cáo kiểm thử tích hợp Scheduler — DormHub

Ngày kiểm thử: 25/11/2026, Asia/Ho_Chi_Minh. Bằng chứng thời gian log: 2026-11-25T20:57:00.854+07:00 đến 2026-11-25T21:10:16.251+07:00.

## Môi trường và phạm vi

- Backend: `D:\WorkSpace\Nam4\WWW\PROJECT_JAVA\Backend-DormHub`; PostgreSQL 18.6 local localhost:5432; database phát triển `dormhub_test_seed` chỉ đọc schema; database test `dormhub_scheduler_test` do đợt này tạo mới từ schema-only dump.

- Không sao chép dữ liệu cá nhân. Chỉ dùng tài khoản @example.invalid, hash không dùng được hoặc mật khẩu ngẫu nhiên chỉ trong bộ nhớ cho ca API. Không có giao dịch ngân hàng thật.

- Java runtime: Eclipse Adoptium 25.0.5; project toolchain Java 21; Gradle wrapper 9.5.1. `bootJar` thành công.

- Tiến trình test bind 127.0.0.1:18081, profile scheduler-test. Mỗi tiến trình chỉ bật đúng một cron 10 giây, các cron còn lại `-`, startup false trừ ca startup true.

- Chạy bằng JAR build từ source hiện tại. Không sửa Java source, Entity, Enum, migration, Web, Mobile hoặc Java test. Không thao tác Git.

- External isolation: mail localhost:9; SePay transactions disabled và URL localhost:9; key dummy; AI localhost:9; không gọi webhook/ngân hàng. NotificationService ghi database, không gửi email.

- Evidence/scripts: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa`. SQL write helper chỉ cho database `dormhub_scheduler_test` và xác minh current_database trước mỗi lệnh ghi. Giữ database và bằng chứng, không DROP.

## Lỗi môi trường profile và giới hạn kết luận

**High — ENV-PROFILE:** file application-scheduler-test.yml hiện không có trong resources/package. Chỉ truyền active profile không đủ để tắt job; các default cron Java và startup true vẫn có thể áp dụng. Không thử khởi động chế độ nguy hiểm đó trên dữ liệu hiện có.

Để tiếp tục kiểm thử trong phạm vi cho phép, harness tạo application-scheduler-test.properties bên ngoài source, chứa đủ 10 cron `-` và startup false, rồi nạp qua spring.config.additional-location. Kết quả runtime dưới đây xác nhận cơ chế property Java với cấu hình test bên ngoài; không xác nhận artifact hiện tại tự an toàn chỉ bằng active profile.

Đề xuất sửa tối thiểu sau khi được phê duyệt: khôi phục file profile trong resources, kiểm tra file có trong JAR. Chưa sửa lỗi source/config của ứng dụng.

## Tổng hợp

- Tổng test case: **80**; PASS **79**; FAIL **1**; BLOCKED **0**; NOT RUN **0**.

- Tỷ lệ PASS trên ca có kết luận PASS/FAIL: **98.75%**.

| Nhóm | PASS | FAIL | BLOCKED | NOT RUN |

|---|---:|---:|---:|---:|

| ENV | 0 | 1 | 0 | 0 |

| EX | 9 | 0 | 0 | 0 |

| S01 | 9 | 0 | 0 | 0 |

| S02 | 9 | 0 | 0 | 0 |

| S03 | 8 | 0 | 0 | 0 |

| S04 | 8 | 0 | 0 | 0 |

| S05 | 6 | 0 | 0 | 0 |

| S06 | 6 | 0 | 0 | 0 |

| S07 | 7 | 0 | 0 | 0 |

| S08 | 5 | 0 | 0 | 0 |

| S09 | 5 | 0 | 0 | 0 |

| S10 | 7 | 0 | 0 | 0 |

## Cách thực thi và đọc bằng chứng

Fixture của từng nhóm có tài khoản/phòng/hợp đồng riêng. SELECT trước được lưu trong before; SELECT sau ít nhất 3 lượt END được lưu trong after. Không kết luận từ log END đơn lẻ. Kiểm tra số hóa đơn/thông báo, trạng thái và occupancy trên PostgreSQL sau khi transaction đã hoàn tất.

Câu SELECT cụ thể và INSERT fixture nằm trong suite.py/extras.py bên ngoài source; dữ liệu kết quả đầy đủ ở results.json. Billing chạy bù có thể nhìn thấy fixture các nhóm trước, nhưng assertion mỗi ca được giới hạn theo contract/invoice/refId của fixture đó.

Các ca `-ISO` xác minh không có method khác ghi START trong tiến trình. Baseline chạy 22 giây không có START; các nhóm chạy tối thiểu 3 lượt. Các API bổ sung dùng HTTP localhost, CookieJar, CSRF và đăng nhập thực; mật khẩu/cookie/token không ghi vào kết quả.

## Chi tiết từng ca

### S01-now — PASS

- Mục tiêu: now

- Expected: Đúng kỳ, đúng proration/full rent, detail=total, không trùng sau 3 lượt

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S01.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 3,
    "billing_month": "2026-11-01",
    "total_amount": 233333,
    "start_date": "2026-11-21T00:00:00",
    "end_date": "2027-11-25T00:00:00",
    "monthly_rent": 700000,
    "detail_total": 233333
  }
]

```

### S01-future — PASS

- Mục tiêu: future

- Expected: Không có hóa đơn

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S01.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S01-pending — PASS

- Mục tiêu: pending

- Expected: Không có hóa đơn

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S01.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S01-expired — PASS

- Mục tiêu: expired

- Expected: Không có hóa đơn

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S01.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S01-cancel — PASS

- Mục tiêu: cancel

- Expected: Không có hóa đơn

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S01.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S01-history — PASS

- Mục tiêu: history

- Expected: Đúng kỳ, đúng proration/full rent, detail=total, không trùng sau 3 lượt

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S01.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 2,
    "billing_month": "2026-10-01",
    "total_amount": 632258,
    "start_date": "2026-10-04T00:00:00",
    "end_date": "2026-12-31T00:00:00",
    "monthly_rent": 700000,
    "detail_total": 632258
  },
  {
    "invoice_id": 4,
    "billing_month": "2026-11-01",
    "total_amount": 700000,
    "start_date": "2026-10-04T00:00:00",
    "end_date": "2026-12-31T00:00:00",
    "monthly_rent": 700000,
    "detail_total": 700000
  }
]

```

### S01-existing — PASS

- Mục tiêu: existing

- Expected: Giữ đúng một hóa đơn hiện có

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S01.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "invoice_id": 1,
    "invoice_type": "ROOM_FEE",
    "billing_month": "2026-11-01",
    "status": "UNPAID",
    "total_amount": 700000
  }
]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 1,
    "billing_month": "2026-11-01",
    "total_amount": 700000,
    "start_date": "2026-11-01T00:00:00",
    "end_date": "2027-11-25T00:00:00",
    "monthly_rent": 700000,
    "detail_total": 0
  }
]

```

### S01-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S01.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S02-now — PASS

- Mục tiêu: now

- Expected: Đúng kỳ, đúng proration/full rent, detail=total, không trùng sau 3 lượt

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S02.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 7,
    "billing_month": "2026-11-01",
    "total_amount": 233333,
    "start_date": "2026-11-21T00:00:00",
    "end_date": "2027-11-25T00:00:00",
    "monthly_rent": 700000,
    "detail_total": 233333
  }
]

```

### S02-future — PASS

- Mục tiêu: future

- Expected: Không có hóa đơn

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S02.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S02-pending — PASS

- Mục tiêu: pending

- Expected: Không có hóa đơn

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S02.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S02-expired — PASS

- Mục tiêu: expired

- Expected: Không có hóa đơn

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S02.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S02-cancel — PASS

- Mục tiêu: cancel

- Expected: Không có hóa đơn

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S02.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S02-history — PASS

- Mục tiêu: history

- Expected: Đúng kỳ, đúng proration/full rent, detail=total, không trùng sau 3 lượt

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S02.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 6,
    "billing_month": "2026-10-01",
    "total_amount": 632258,
    "start_date": "2026-10-04T00:00:00",
    "end_date": "2026-12-31T00:00:00",
    "monthly_rent": 700000,
    "detail_total": 632258
  },
  {
    "invoice_id": 8,
    "billing_month": "2026-11-01",
    "total_amount": 700000,
    "start_date": "2026-10-04T00:00:00",
    "end_date": "2026-12-31T00:00:00",
    "monthly_rent": 700000,
    "detail_total": 700000
  }
]

```

### S02-existing — PASS

- Mục tiêu: existing

- Expected: Giữ đúng một hóa đơn hiện có

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S02.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "invoice_id": 5,
    "invoice_type": "ROOM_FEE",
    "billing_month": "2026-11-01",
    "status": "UNPAID",
    "total_amount": 700000
  }
]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 5,
    "billing_month": "2026-11-01",
    "total_amount": 700000,
    "start_date": "2026-11-01T00:00:00",
    "end_date": "2027-11-25T00:00:00",
    "monthly_rent": 700000,
    "detail_total": 0
  }
]

```

### S02-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S02.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S03-notdue — PASS

- Mục tiêu: notdue

- Expected: Hủy đúng ca overdue; giữ các ca được bảo vệ; không trùng thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S03.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 9,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

- Actual / SELECT sau:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 9,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

### S03-grace — PASS

- Mục tiêu: grace

- Expected: Hủy đúng ca overdue; giữ các ca được bảo vệ; không trùng thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S03.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 10,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

- Actual / SELECT sau:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 10,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

### S03-overdue — PASS

- Mục tiêu: overdue

- Expected: Hủy đúng ca overdue; giữ các ca được bảo vệ; không trùng thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S03.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 11,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

- Actual / SELECT sau:

```json

{
  "contract": [
    {
      "status": "CANCELLED",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 11,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "CANCELLED",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "CANCELLED"
    }
  ],
  "notifications": [
    {
      "id": 7,
      "type": "GENERAL",
      "ref_id": 17,
      "user_id": 18
    }
  ]
}

```

### S03-success — PASS

- Mục tiêu: success

- Expected: Hủy đúng ca overdue; giữ các ca được bảo vệ; không trùng thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S03.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 12,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

- Actual / SELECT sau:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 12,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

### S03-pending — PASS

- Mục tiêu: pending

- Expected: Hủy đúng ca overdue; giữ các ca được bảo vệ; không trùng thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S03.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 13,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

- Actual / SELECT sau:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 13,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

### S03-unmatched — PASS

- Mục tiêu: unmatched

- Expected: Hủy đúng ca overdue; giữ các ca được bảo vệ; không trùng thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S03.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 14,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

- Actual / SELECT sau:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 14,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

### S03-review — PASS

- Mục tiêu: review

- Expected: Hủy đúng ca overdue; giữ các ca được bảo vệ; không trùng thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S03.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 15,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

- Actual / SELECT sau:

```json

{
  "contract": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "invoice": [
    {
      "invoice_id": 15,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ],
  "application": [
    {
      "status": "APPROVED"
    }
  ],
  "notifications": []
}

```

### S03-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S03.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S04-match — PASS

- Mục tiêu: match

- Expected: MATCHED; occupancy/assignment không tăng hai lần

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S04.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "transaction": [
    {
      "id": 3,
      "status": "UNMATCHED",
      "payment_id": 5,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 16,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "transaction": [
    {
      "id": 3,
      "status": "MATCHED",
      "payment_id": 5,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "AVAILABLE",
      "assignments": 1
    }
  ],
  "payment": [
    {
      "status": "SUCCESS",
      "paid_at": "2026-11-25T20:00:54.279125+07:00"
    }
  ],
  "invoice": [
    {
      "invoice_id": 16,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "PAID",
      "total_amount": 700000
    }
  ]
}

```

### S04-nopay — PASS

- Mục tiêu: nopay

- Expected: UNMATCHED; occupancy/assignment không tăng hai lần

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S04.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "transaction": [
    {
      "id": 4,
      "status": "UNMATCHED",
      "payment_id": null,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [],
  "invoice": [
    {
      "invoice_id": 17,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "transaction": [
    {
      "id": 4,
      "status": "UNMATCHED",
      "payment_id": null,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [],
  "invoice": [
    {
      "invoice_id": 17,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

### S04-amount — PASS

- Mục tiêu: amount

- Expected: REVIEW_REQUIRED; occupancy/assignment không tăng hai lần

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S04.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "transaction": [
    {
      "id": 5,
      "status": "UNMATCHED",
      "payment_id": 6,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 18,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "transaction": [
    {
      "id": 5,
      "status": "REVIEW_REQUIRED",
      "payment_id": 6,
      "review_reason": "Amount or payment method does not match"
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 18,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

### S04-late — PASS

- Mục tiêu: late

- Expected: REVIEW_REQUIRED; occupancy/assignment không tăng hai lần

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S04.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "transaction": [
    {
      "id": 6,
      "status": "UNMATCHED",
      "payment_id": 7,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 19,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "transaction": [
    {
      "id": 6,
      "status": "REVIEW_REQUIRED",
      "payment_id": 7,
      "review_reason": "Bank transfer occurred after payment expiry"
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 19,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

### S04-matched — PASS

- Mục tiêu: matched

- Expected: MATCHED; occupancy/assignment không tăng hai lần

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S04.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "transaction": [
    {
      "id": 7,
      "status": "MATCHED",
      "payment_id": 8,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 20,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "transaction": [
    {
      "id": 7,
      "status": "MATCHED",
      "payment_id": 8,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 20,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

### S04-review — PASS

- Mục tiêu: review

- Expected: REVIEW_REQUIRED; occupancy/assignment không tăng hai lần

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S04.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "transaction": [
    {
      "id": 8,
      "status": "REVIEW_REQUIRED",
      "payment_id": 9,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 21,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "transaction": [
    {
      "id": 8,
      "status": "REVIEW_REQUIRED",
      "payment_id": 9,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 21,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

### S04-old — PASS

- Mục tiêu: old

- Expected: UNMATCHED; occupancy/assignment không tăng hai lần

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S04.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "transaction": [
    {
      "id": 9,
      "status": "UNMATCHED",
      "payment_id": 10,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 22,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "transaction": [
    {
      "id": 9,
      "status": "UNMATCHED",
      "payment_id": 10,
      "review_reason": null
    }
  ],
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "UNPAID",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 0
    }
  ],
  "payment": [
    {
      "status": "PENDING",
      "paid_at": null
    }
  ],
  "invoice": [
    {
      "invoice_id": 22,
      "invoice_type": "DEPOSIT",
      "billing_month": null,
      "status": "UNPAID",
      "total_amount": 700000
    }
  ]
}

```

### S04-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S04.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S05-today — PASS

- Mục tiêu: today

- Expected: Một thông báo đúng người nhận

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S05.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "id": 24,
    "type": "INVOICE_DUE_REMINDER",
    "ref_id": 23,
    "user_id": 30
  }
]

```

### S05-day3 — PASS

- Mục tiêu: day3

- Expected: Một thông báo đúng người nhận

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S05.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "id": 25,
    "type": "INVOICE_DUE_REMINDER",
    "ref_id": 24,
    "user_id": 31
  }
]

```

### S05-day4 — PASS

- Mục tiêu: day4

- Expected: Không có thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S05.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S05-paid — PASS

- Mục tiêu: paid

- Expected: Không có thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S05.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S05-cancel — PASS

- Mục tiêu: cancel

- Expected: Không có thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S05.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S05-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S05.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S06-yesterday — PASS

- Mục tiêu: yesterday

- Expected: Một thông báo đúng người nhận

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S06.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "id": 26,
    "type": "INVOICE_OVERDUE",
    "ref_id": 28,
    "user_id": 35
  }
]

```

### S06-future — PASS

- Mục tiêu: future

- Expected: Không có thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S06.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S06-paid — PASS

- Mục tiêu: paid

- Expected: Không có thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S06.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S06-todaypast — PASS

- Mục tiêu: todaypast

- Expected: Không có thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S06.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S06-outside — PASS

- Mục tiêu: outside

- Expected: Không có thông báo

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S06.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S06-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S06.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S07-soon — PASS

- Mục tiêu: soon

- Expected: Đúng một thông báo nếu soon/đã nhắc; không gửi lại sau gia hạn cùng contractId

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S07.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "id": 29,
    "type": "CONTRACT_EXPIRING",
    "ref_id": 39,
    "user_id": 40
  }
]

```

### S07-later — PASS

- Mục tiêu: later

- Expected: Đúng một thông báo nếu soon/đã nhắc; không gửi lại sau gia hạn cùng contractId

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S07.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S07-pending — PASS

- Mục tiêu: pending

- Expected: Đúng một thông báo nếu soon/đã nhắc; không gửi lại sau gia hạn cùng contractId

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S07.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S07-cancel — PASS

- Mục tiêu: cancel

- Expected: Đúng một thông báo nếu soon/đã nhắc; không gửi lại sau gia hạn cùng contractId

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S07.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S07-existing — PASS

- Mục tiêu: existing

- Expected: Đúng một thông báo nếu soon/đã nhắc; không gửi lại sau gia hạn cùng contractId

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S07.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "id": 27,
    "type": "CONTRACT_EXPIRING",
    "ref_id": 43,
    "user_id": 44
  }
]

```

- Actual / SELECT sau:

```json

[
  {
    "id": 27,
    "type": "CONTRACT_EXPIRING",
    "ref_id": 43,
    "user_id": 44
  }
]

```

### S07-renewed — PASS

- Mục tiêu: renewed

- Expected: Đúng một thông báo nếu soon/đã nhắc; không gửi lại sau gia hạn cùng contractId

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S07.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "id": 28,
    "type": "CONTRACT_EXPIRING",
    "ref_id": 44,
    "user_id": 45
  }
]

```

- Actual / SELECT sau:

```json

[
  {
    "id": 28,
    "type": "CONTRACT_EXPIRING",
    "ref_id": 44,
    "user_id": 45
  }
]

```

### S07-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S07.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S08-past — PASS

- Mục tiêu: past

- Expected: ACTIVE quá hạn -> EXPIRED; assignment/occupancy giữ nguyên

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S08.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "state": [
    {
      "status": "ACTIVE",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "CHECKED_IN"
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "state": [
    {
      "status": "EXPIRED",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "CHECKED_IN"
    }
  ]
}

```

### S08-future — PASS

- Mục tiêu: future

- Expected: ACTIVE quá hạn -> EXPIRED; assignment/occupancy giữ nguyên

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S08.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "state": [
    {
      "status": "ACTIVE",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "CHECKED_IN"
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "state": [
    {
      "status": "ACTIVE",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "CHECKED_IN"
    }
  ]
}

```

### S08-pending — PASS

- Mục tiêu: pending

- Expected: ACTIVE quá hạn -> EXPIRED; assignment/occupancy giữ nguyên

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S08.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "CHECKED_IN"
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "CHECKED_IN"
    }
  ]
}

```

### S08-expired — PASS

- Mục tiêu: expired

- Expected: ACTIVE quá hạn -> EXPIRED; assignment/occupancy giữ nguyên

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S08.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "state": [
    {
      "status": "EXPIRED",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "CHECKED_IN"
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "state": [
    {
      "status": "EXPIRED",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "CHECKED_IN"
    }
  ]
}

```

### S08-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S08.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S09-oldread — PASS

- Mục tiêu: oldread

- Expected: Xóa cha/con nếu cũ; giữ mới; không FK error

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S09.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "notification": [
    {
      "id": 30
    }
  ],
  "receivers": [
    {
      "id": 30
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "notification": [],
  "receivers": []
}

```

### S09-oldunread — PASS

- Mục tiêu: oldunread

- Expected: Xóa cha/con nếu cũ; giữ mới; không FK error

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S09.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "notification": [
    {
      "id": 31
    }
  ],
  "receivers": [
    {
      "id": 31
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "notification": [],
  "receivers": []
}

```

### S09-newread — PASS

- Mục tiêu: newread

- Expected: Xóa cha/con nếu cũ; giữ mới; không FK error

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S09.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "notification": [
    {
      "id": 32
    }
  ],
  "receivers": [
    {
      "id": 32
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "notification": [
    {
      "id": 32
    }
  ],
  "receivers": [
    {
      "id": 32
    }
  ]
}

```

### S09-newunread — PASS

- Mục tiêu: newunread

- Expected: Xóa cha/con nếu cũ; giữ mới; không FK error

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S09.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "notification": [
    {
      "id": 33
    }
  ],
  "receivers": [
    {
      "id": 33
    }
  ]
}

```

- Actual / SELECT sau:

```json

{
  "notification": [
    {
      "id": 33
    }
  ],
  "receivers": [
    {
      "id": 33
    }
  ]
}

```

### S09-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S09.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### S10-refresh-expired — PASS

- Mục tiêu: refresh expired

- Expected: Chỉ giữ bản ghi còn hiệu lực

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S10.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "id": 1
  }
]

```

- Actual / SELECT sau:

```json

[]

```

### S10-refresh-used — PASS

- Mục tiêu: refresh used

- Expected: Chỉ giữ bản ghi còn hiệu lực

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S10.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "id": 2
  }
]

```

- Actual / SELECT sau:

```json

[]

```

### S10-refresh-valid — PASS

- Mục tiêu: refresh valid

- Expected: Chỉ giữ bản ghi còn hiệu lực

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S10.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "id": 3
  }
]

```

- Actual / SELECT sau:

```json

[
  {
    "id": 3
  }
]

```

### S10-otp-expired — PASS

- Mục tiêu: otp expired

- Expected: Chỉ giữ bản ghi còn hiệu lực

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S10.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "id": 1
  }
]

```

- Actual / SELECT sau:

```json

[]

```

### S10-otp-used — PASS

- Mục tiêu: otp used

- Expected: Chỉ giữ bản ghi còn hiệu lực

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S10.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "id": 2
  }
]

```

- Actual / SELECT sau:

```json

[]

```

### S10-otp-valid — PASS

- Mục tiêu: otp valid

- Expected: Chỉ giữ bản ghi còn hiệu lực

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S10.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "id": 3
  }
]

```

- Actual / SELECT sau:

```json

[
  {
    "id": 3
  }
]

```

### S10-ISO — PASS

- Mục tiêu: Chỉ Scheduler được chọn chạy

- Expected: []

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S10.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### EX-startup-false — PASS

- Mục tiêu: Startup reconciliation false và cả 10 lịch tắt

- Expected: Không có hóa đơn, không START, có disabled startup

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\startup-false.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[]

```

### EX-startup-true-1 — PASS

- Mục tiêu: Startup reconciliation true / restart

- Expected: Đúng 1 hóa đơn, không trùng sau restart

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\startup-true-1.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 38,
    "invoice_type": "ROOM_FEE",
    "billing_month": "2026-11-01",
    "status": "UNPAID",
    "total_amount": 700000
  }
]

```

### EX-startup-true-2 — PASS

- Mục tiêu: Startup reconciliation true / restart

- Expected: Đúng 1 hóa đơn, không trùng sau restart

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\startup-true-2.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "invoice_id": 38,
    "invoice_type": "ROOM_FEE",
    "billing_month": "2026-11-01",
    "status": "UNPAID",
    "total_amount": 700000
  }
]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 38,
    "invoice_type": "ROOM_FEE",
    "billing_month": "2026-11-01",
    "status": "UNPAID",
    "total_amount": 700000
  }
]

```

### S01-history-count — PASS

- Mục tiêu: Đủ cả kỳ trước và kỳ hiện tại

- Expected: Đúng 2 kỳ tháng riêng biệt

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S01.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 2,
    "invoice_type": "ROOM_FEE",
    "billing_month": "2026-10-01",
    "status": "UNPAID",
    "total_amount": 632258
  },
  {
    "invoice_id": 4,
    "invoice_type": "ROOM_FEE",
    "billing_month": "2026-11-01",
    "status": "UNPAID",
    "total_amount": 700000
  }
]

```

### S02-history-count — PASS

- Mục tiêu: Đủ cả kỳ trước và kỳ hiện tại

- Expected: Đúng 2 kỳ tháng riêng biệt

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\S02.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[]

```

- Actual / SELECT sau:

```json

[
  {
    "invoice_id": 6,
    "invoice_type": "ROOM_FEE",
    "billing_month": "2026-10-01",
    "status": "UNPAID",
    "total_amount": 632258
  },
  {
    "invoice_id": 8,
    "invoice_type": "ROOM_FEE",
    "billing_month": "2026-11-01",
    "status": "UNPAID",
    "total_amount": 700000
  }
]

```

### EX-renewed-reminder — PASS

- Mục tiêu: Gia hạn cùng contractId sau lần nhắc

- Expected: Không trùng; ghi nhận giới hạn không nhắc lại kỳ gia hạn

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\renewal-reminder.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "id": 28,
    "type": "CONTRACT_EXPIRING",
    "ref_id": 44,
    "user_id": 45
  }
]

```

- Actual / SELECT sau:

```json

[
  {
    "id": 28,
    "type": "CONTRACT_EXPIRING",
    "ref_id": 44,
    "user_id": 45
  }
]

```

### EX-date-outside — PASS

- Mục tiêu: Ngày ngoài 7 ngày / đã qua hạn

- Expected: Từ chối; PENDING/HELD, assignment và occupancy giữ nguyên

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\extra-api.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "status": "PENDING",
    "deposit_status": "HELD",
    "current_occupancy": 1,
    "room_status": "OCCUPIED",
    "assignments": 1
  }
]

```

- Actual / SELECT sau:

```json

{
  "http": 400,
  "code": "INVALID_REQUEST",
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ]
}

```

### EX-date-elapsed — PASS

- Mục tiêu: Ngày ngoài 7 ngày / đã qua hạn

- Expected: Từ chối; PENDING/HELD, assignment và occupancy giữ nguyên

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\extra-api.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "status": "PENDING",
    "deposit_status": "HELD",
    "current_occupancy": 1,
    "room_status": "OCCUPIED",
    "assignments": 1
  }
]

```

- Actual / SELECT sau:

```json

{
  "http": 400,
  "code": "INVALID_REQUEST",
  "state": [
    {
      "status": "PENDING",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "OCCUPIED",
      "assignments": 1
    }
  ]
}

```

### EX-checkout-expired — PASS

- Mục tiêu: Tạo yêu cầu trả phòng sau Scheduler hết hạn

- Expected: HTTP 201, tạo yêu cầu PENDING

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\extra-api.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "status": "EXPIRED",
    "deposit_status": "HELD",
    "current_occupancy": 1,
    "room_status": "OCCUPIED",
    "assignments": 1
  }
]

```

- Actual / SELECT sau:

```json

{
  "http": 201,
  "code": "SUCCESS",
  "data": {
    "checkoutRequestId": 1,
    "studentId": 45,
    "contractId": 45,
    "roomId": 45,
    "requestDate": "2026-11-25T21:08:54.2630783",
    "checkoutDate": null,
    "status": "PENDING",
    "reason": "Scheduler QA expired contract checkout",
    "processedAt": null,
    "note": null,
    "reviewedByName": null,
    "depositStatus": "HELD"
  }
}

```

### EX-date-valid — PASS

- Mục tiêu: Chọn ngày hợp lệ, đồng bộ CSRF trước mutation

- Expected: HTTP 200, ACTIVE/HELD, ASSIGNED/occupancy 1

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\api-fresh-csrf.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "status": "PENDING",
    "deposit_status": "HELD",
    "current_occupancy": 1,
    "room_status": "OCCUPIED",
    "assignments": 1
  }
]

```

- Actual / SELECT sau:

```json

{
  "http": [
    200,
    200
  ],
  "code": "SUCCESS",
  "plan": {
    "contractId": 50,
    "roomNumber": "EX-valid",
    "buildingName": "Scheduler QA",
    "depositPaidAt": "2026-11-25T21:07:53.043286",
    "earliestDate": "2026-11-25",
    "latestDate": "2026-12-02",
    "checkInDate": "2026-11-25",
    "startDate": "2026-11-25",
    "endDate": "2027-11-30",
    "monthlyRent": 700000,
    "firstMonthRent": 140000,
    "confirmed": false
  },
  "state": [
    {
      "status": "ACTIVE",
      "deposit_status": "HELD",
      "current_occupancy": 1,
      "room_status": "AVAILABLE",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "ASSIGNED"
    }
  ]
}

```

### EX-checkout-complete — PASS

- Mục tiêu: Hoàn tất Checkout hợp đồng EXPIRED với CSRF mới

- Expected: HTTP 200, CHECKED_OUT, occupancy=0

- Log: `C:\Users\ACER\AppData\Local\Temp\dormhub_scheduler_qa\api-fresh-csrf.log`

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

[
  {
    "status": "EXPIRED",
    "deposit_status": "HELD",
    "current_occupancy": 1,
    "room_status": "OCCUPIED",
    "assignments": 1
  }
]

```

- Actual / SELECT sau:

```json

{
  "http": 200,
  "code": "SUCCESS",
  "state": [
    {
      "status": "EXPIRED",
      "deposit_status": "REFUNDED",
      "current_occupancy": 0,
      "room_status": "AVAILABLE",
      "assignments": 1
    }
  ],
  "assignment": [
    {
      "status": "CHECKED_OUT"
    }
  ]
}

```

### ENV-PROFILE — FAIL

- Mục tiêu: Profile scheduler-test có trong source/package mặc định

- Expected: Profile mặc định tắt 10 cron và startup nằm trong package

- Evidence: Kiểm tra filesystem và ZIP entries của JAR; không chạy cấu hình không an toàn

- Thực hiện: tạo fixture riêng → SELECT trước → bật đúng cron (hoặc API/startup theo tên ca) → chờ các lượt chạy → SELECT sau → so sánh.

- SELECT trước / dữ liệu đầu vào:

```json

{
  "source_yml": false
}

```

- Actual / SELECT sau:

```json

{
  "packaged_profile": false
}

```

## Log trích dẫn theo dòng

### api-fresh-csrf.log

```text

1418: 2026-11-25T21:10:13.388+07:00  INFO 23740 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1430: 2026-11-25T21:10:13.873+07:00  INFO 23740 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 139 ms

1435: 2026-11-25T21:10:14.397+07:00 DEBUG 23740 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1440: 2026-11-25T21:10:14.427+07:00 DEBUG 23740 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1447: 2026-11-25T21:10:14.862+07:00 DEBUG 23740 --- [.1-18081-exec-9] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1452: 2026-11-25T21:10:14.953+07:00 DEBUG 23740 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1457: 2026-11-25T21:10:15.262+07:00 DEBUG 23740 --- [.1-18081-exec-5] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1462: 2026-11-25T21:10:15.286+07:00 DEBUG 23740 --- [.1-18081-exec-6] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1469: 2026-11-25T21:10:15.417+07:00 DEBUG 23740 --- [.1-18081-exec-7] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1474: 2026-11-25T21:10:15.796+07:00 DEBUG 23740 --- [.1-18081-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1479: 2026-11-25T21:10:15.811+07:00 DEBUG 23740 --- [.1-18081-exec-3] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1486: 2026-11-25T21:10:15.910+07:00 DEBUG 23740 --- [1-18081-exec-10] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1491: 2026-11-25T21:10:15.941+07:00 DEBUG 23740 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1496: 2026-11-25T21:10:16.128+07:00 DEBUG 23740 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1504: 2026-11-25T21:10:16.251+07:00 DEBUG 23740 --- [.1-18081-exec-9] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

```

### baseline.log

```text

1418: 2026-11-25T20:57:00.854+07:00  INFO 17720 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

```

### extra-api.log

```text

1418: 2026-11-25T21:08:50.609+07:00  INFO 2920 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1430: 2026-11-25T21:08:51.338+07:00  INFO 2920 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed initialization in 106 ms

1435: 2026-11-25T21:08:51.821+07:00 DEBUG 2920 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1442: 2026-11-25T21:08:52.252+07:00 DEBUG 2920 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1447: 2026-11-25T21:08:52.337+07:00 DEBUG 2920 --- [.1-18081-exec-6] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1452: 2026-11-25T21:08:52.665+07:00 DEBUG 2920 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1457: 2026-11-25T21:08:52.867+07:00 DEBUG 2920 --- [.1-18081-exec-3] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1464: 2026-11-25T21:08:52.966+07:00 DEBUG 2920 --- [.1-18081-exec-7] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1469: 2026-11-25T21:08:52.975+07:00 DEBUG 2920 --- [.1-18081-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1478: 2026-11-25T21:08:53.255+07:00 DEBUG 2920 --- [.1-18081-exec-9] o.s.web.servlet.DispatcherServlet        : Completed 400 BAD_REQUEST

1483: 2026-11-25T21:08:53.458+07:00 DEBUG 2920 --- [1-18081-exec-10] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1490: 2026-11-25T21:08:53.560+07:00 DEBUG 2920 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1495: 2026-11-25T21:08:53.570+07:00 DEBUG 2920 --- [.1-18081-exec-1] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1504: 2026-11-25T21:08:53.699+07:00 DEBUG 2920 --- [.1-18081-exec-6] o.s.web.servlet.DispatcherServlet        : Completed 400 BAD_REQUEST

1509: 2026-11-25T21:08:53.894+07:00 DEBUG 2920 --- [.1-18081-exec-4] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1516: 2026-11-25T21:08:53.992+07:00 DEBUG 2920 --- [.1-18081-exec-5] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1521: 2026-11-25T21:08:54.001+07:00 DEBUG 2920 --- [.1-18081-exec-3] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1527: 2026-11-25T21:08:54.271+07:00 DEBUG 2920 --- [.1-18081-exec-7] o.s.web.servlet.DispatcherServlet        : Completed 201 CREATED

1532: 2026-11-25T21:08:54.390+07:00 DEBUG 2920 --- [.1-18081-exec-8] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1539: 2026-11-25T21:08:54.474+07:00 DEBUG 2920 --- [.1-18081-exec-9] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1544: 2026-11-25T21:08:54.502+07:00 DEBUG 2920 --- [1-18081-exec-10] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

1551: 2026-11-25T21:08:54.570+07:00 DEBUG 2920 --- [.1-18081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed 200 OK

```

### renewal-reminder.log

```text

1418: 2026-11-25T21:07:24.351+07:00  INFO 18796 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:07:30.005+07:00  INFO 18796 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts START at=2026-11-25T14:07:30.005652300Z

1422: 2026-11-25T21:07:30.258+07:00  INFO 18796 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts END at=2026-11-25T14:07:30.258921400Z

1423: 2026-11-25T21:07:40.002+07:00  INFO 18796 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts START at=2026-11-25T14:07:40.002040300Z

1425: 2026-11-25T21:07:40.018+07:00  INFO 18796 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts END at=2026-11-25T14:07:40.018059300Z

1426: 2026-11-25T21:07:50.001+07:00  INFO 18796 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts START at=2026-11-25T14:07:50.001904900Z

1428: 2026-11-25T21:07:50.015+07:00  INFO 18796 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts END at=2026-11-25T14:07:50.015927900Z

```

### S01.log

```text

1418: 2026-11-25T20:58:57.932+07:00  INFO 23648 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T20:59:00.006+07:00  INFO 23648 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateMonthlyRoomFees START at=2026-11-25T13:59:00.006213800Z

1421: 2026-11-25T20:59:00.398+07:00  INFO 23648 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-10, created=1, skipped=0, failed=0

1422: 2026-11-25T20:59:00.441+07:00  INFO 23648 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-11, created=2, skipped=1, failed=0

1423: 2026-11-25T20:59:00.442+07:00  INFO 23648 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateMonthlyRoomFees END at=2026-11-25T13:59:00.442471300Z

1424: 2026-11-25T20:59:10.001+07:00  INFO 23648 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateMonthlyRoomFees START at=2026-11-25T13:59:10.001892800Z

1425: 2026-11-25T20:59:10.012+07:00  INFO 23648 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-10, created=0, skipped=1, failed=0

1426: 2026-11-25T20:59:10.026+07:00  INFO 23648 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-11, created=0, skipped=3, failed=0

1427: 2026-11-25T20:59:10.026+07:00  INFO 23648 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateMonthlyRoomFees END at=2026-11-25T13:59:10.026220500Z

1428: 2026-11-25T20:59:20.000+07:00  INFO 23648 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateMonthlyRoomFees START at=2026-11-25T13:59:20.000949700Z

1429: 2026-11-25T20:59:20.010+07:00  INFO 23648 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-10, created=0, skipped=1, failed=0

1430: 2026-11-25T20:59:20.023+07:00  INFO 23648 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-11, created=0, skipped=3, failed=0

1431: 2026-11-25T20:59:20.023+07:00  INFO 23648 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.generateMonthlyRoomFees END at=2026-11-25T13:59:20.023906200Z

```

### S02.log

```text

1418: 2026-11-25T20:59:36.932+07:00  INFO 10676 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T20:59:40.006+07:00  INFO 10676 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileDailyRoomFees START at=2026-11-25T13:59:40.006731900Z

1421: 2026-11-25T20:59:40.400+07:00  INFO 10676 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-10, created=1, skipped=1, failed=0

1422: 2026-11-25T20:59:40.460+07:00  INFO 10676 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-11, created=2, skipped=4, failed=0

1423: 2026-11-25T20:59:40.460+07:00  INFO 10676 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileDailyRoomFees END at=2026-11-25T13:59:40.460389700Z

1424: 2026-11-25T20:59:50.001+07:00  INFO 10676 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileDailyRoomFees START at=2026-11-25T13:59:50.001059700Z

1425: 2026-11-25T20:59:50.012+07:00  INFO 10676 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-10, created=0, skipped=2, failed=0

1426: 2026-11-25T20:59:50.033+07:00  INFO 10676 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-11, created=0, skipped=6, failed=0

1427: 2026-11-25T20:59:50.034+07:00  INFO 10676 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileDailyRoomFees END at=2026-11-25T13:59:50.034265900Z

1428: 2026-11-25T21:00:00.000+07:00  INFO 10676 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileDailyRoomFees START at=2026-11-25T14:00:00.000661900Z

1429: 2026-11-25T21:00:00.013+07:00  INFO 10676 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-10, created=0, skipped=2, failed=0

1430: 2026-11-25T21:00:00.044+07:00  INFO 10676 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-11, created=0, skipped=6, failed=0

1431: 2026-11-25T21:00:00.045+07:00  INFO 10676 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileDailyRoomFees END at=2026-11-25T14:00:00.045386900Z

```

### S03.log

```text

1418: 2026-11-25T21:00:22.217+07:00  INFO 30256 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:00:30.007+07:00  INFO 30256 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireUnpaidDeposits START at=2026-11-25T14:00:30.007146400Z

1427: 2026-11-25T21:00:30.419+07:00  INFO 30256 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Deposit expiry candidates=5, evaluated=5, failed=0 (evaluated does not imply cancelled)

1428: 2026-11-25T21:00:30.419+07:00  INFO 30256 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireUnpaidDeposits END at=2026-11-25T14:00:30.419865100Z

1429: 2026-11-25T21:00:40.000+07:00  INFO 30256 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireUnpaidDeposits START at=2026-11-25T14:00:40.000376700Z

1434: 2026-11-25T21:00:40.051+07:00  INFO 30256 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Deposit expiry candidates=4, evaluated=4, failed=0 (evaluated does not imply cancelled)

1435: 2026-11-25T21:00:40.052+07:00  INFO 30256 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireUnpaidDeposits END at=2026-11-25T14:00:40.052435100Z

1436: 2026-11-25T21:00:50.002+07:00  INFO 30256 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireUnpaidDeposits START at=2026-11-25T14:00:50.002030100Z

1441: 2026-11-25T21:00:50.051+07:00  INFO 30256 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Deposit expiry candidates=4, evaluated=4, failed=0 (evaluated does not imply cancelled)

1442: 2026-11-25T21:00:50.051+07:00  INFO 30256 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireUnpaidDeposits END at=2026-11-25T14:00:50.051069800Z

```

### S04.log

```text

1418: 2026-11-25T21:01:14.642+07:00  INFO 21632 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:01:20.007+07:00  INFO 21632 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions START at=2026-11-25T14:01:20.007497900Z

1424: 2026-11-25T21:01:20.475+07:00  INFO 21632 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Payment reconciliation candidates=5, matched=1

1425: 2026-11-25T21:01:20.475+07:00  INFO 21632 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions END at=2026-11-25T14:01:20.475195700Z

1426: 2026-11-25T21:01:30.001+07:00  INFO 21632 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions START at=2026-11-25T14:01:30.001615600Z

1428: 2026-11-25T21:01:30.009+07:00  INFO 21632 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Payment reconciliation candidates=1, matched=0

1429: 2026-11-25T21:01:30.009+07:00  INFO 21632 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions END at=2026-11-25T14:01:30.009357600Z

1430: 2026-11-25T21:01:40.002+07:00  INFO 21632 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions START at=2026-11-25T14:01:40.002381800Z

1432: 2026-11-25T21:01:40.011+07:00  INFO 21632 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Payment reconciliation candidates=1, matched=0

1433: 2026-11-25T21:01:40.011+07:00  INFO 21632 --- [   scheduling-1] c.d.s.PaymentReconciliationScheduler     : Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions END at=2026-11-25T14:01:40.011896100Z

```

### S05.log

```text

1418: 2026-11-25T21:01:59.679+07:00  INFO 4120 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:02:00.005+07:00  INFO 4120 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendInvoiceDueReminders START at=2026-11-25T14:02:00.005677800Z

1422: 2026-11-25T21:02:00.429+07:00  INFO 4120 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendInvoiceDueReminders END at=2026-11-25T14:02:00.429047900Z

1423: 2026-11-25T21:02:10.001+07:00  INFO 4120 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendInvoiceDueReminders START at=2026-11-25T14:02:10.001128Z

1425: 2026-11-25T21:02:10.039+07:00  INFO 4120 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendInvoiceDueReminders END at=2026-11-25T14:02:10.039178400Z

1426: 2026-11-25T21:02:20.001+07:00  INFO 4120 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendInvoiceDueReminders START at=2026-11-25T14:02:20.001029200Z

1428: 2026-11-25T21:02:20.040+07:00  INFO 4120 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendInvoiceDueReminders END at=2026-11-25T14:02:20.040102900Z

```

### S06.log

```text

1418: 2026-11-25T21:02:36.117+07:00  INFO 23984 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:02:40.007+07:00  INFO 23984 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendOverdueInvoiceReminders START at=2026-11-25T14:02:40.007914700Z

1422: 2026-11-25T21:02:40.332+07:00  INFO 23984 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendOverdueInvoiceReminders END at=2026-11-25T14:02:40.332976Z

1423: 2026-11-25T21:02:50.001+07:00  INFO 23984 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendOverdueInvoiceReminders START at=2026-11-25T14:02:50.001083400Z

1425: 2026-11-25T21:02:50.012+07:00  INFO 23984 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendOverdueInvoiceReminders END at=2026-11-25T14:02:50.012606300Z

1426: 2026-11-25T21:03:00.002+07:00  INFO 23984 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendOverdueInvoiceReminders START at=2026-11-25T14:03:00.002270300Z

1428: 2026-11-25T21:03:00.013+07:00  INFO 23984 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.sendOverdueInvoiceReminders END at=2026-11-25T14:03:00.013882400Z

```

### S07.log

```text

1418: 2026-11-25T21:03:16.262+07:00  INFO 4284 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:03:20.007+07:00  INFO 4284 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts START at=2026-11-25T14:03:20.007199500Z

1422: 2026-11-25T21:03:20.277+07:00  INFO 4284 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts END at=2026-11-25T14:03:20.277188300Z

1423: 2026-11-25T21:03:30.002+07:00  INFO 4284 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts START at=2026-11-25T14:03:30.002338100Z

1425: 2026-11-25T21:03:30.014+07:00  INFO 4284 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts END at=2026-11-25T14:03:30.014996500Z

1426: 2026-11-25T21:03:40.000+07:00  INFO 4284 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts START at=2026-11-25T14:03:40.000596300Z

1428: 2026-11-25T21:03:40.016+07:00  INFO 4284 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.remindExpiringContracts END at=2026-11-25T14:03:40.016250500Z

```

### S08.log

```text

1418: 2026-11-25T21:03:55.389+07:00  INFO 10872 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:04:00.041+07:00  INFO 10872 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireOverdueContracts START at=2026-11-25T14:04:00.041842600Z

1422: 2026-11-25T21:04:00.091+07:00  INFO 10872 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireOverdueContracts END at=2026-11-25T14:04:00.091783600Z

1423: 2026-11-25T21:04:10.002+07:00  INFO 10872 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireOverdueContracts START at=2026-11-25T14:04:10.002232Z

1425: 2026-11-25T21:04:10.004+07:00  INFO 10872 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireOverdueContracts END at=2026-11-25T14:04:10.004849200Z

1426: 2026-11-25T21:04:20.001+07:00  INFO 10872 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireOverdueContracts START at=2026-11-25T14:04:20.001894Z

1428: 2026-11-25T21:04:20.006+07:00  INFO 10872 --- [   scheduling-1] c.d.s.ContractLifecycleScheduler         : Scheduler ContractLifecycleScheduler.expireOverdueContracts END at=2026-11-25T14:04:20.006410600Z

```

### S09.log

```text

1418: 2026-11-25T21:04:51.664+07:00  INFO 16168 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:05:00.029+07:00  INFO 16168 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.cleanupOldNotifications START at=2026-11-25T14:05:00.029426Z

1422: 2026-11-25T21:05:00.097+07:00  INFO 16168 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.cleanupOldNotifications END at=2026-11-25T14:05:00.097735600Z

1423: 2026-11-25T21:05:10.002+07:00  INFO 16168 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.cleanupOldNotifications START at=2026-11-25T14:05:10.002253900Z

1425: 2026-11-25T21:05:10.005+07:00  INFO 16168 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.cleanupOldNotifications END at=2026-11-25T14:05:10.005809400Z

1426: 2026-11-25T21:05:20.002+07:00  INFO 16168 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.cleanupOldNotifications START at=2026-11-25T14:05:20.002092800Z

1428: 2026-11-25T21:05:20.005+07:00  INFO 16168 --- [   scheduling-1] c.d.scheduler.NotificationScheduler      : Scheduler NotificationScheduler.cleanupOldNotifications END at=2026-11-25T14:05:20.005431Z

```

### S10.log

```text

1418: 2026-11-25T21:05:33.284+07:00  INFO 23220 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

1420: 2026-11-25T21:05:40.029+07:00  INFO 23220 --- [   scheduling-1] com.dormhub.service.TokenCleanupTask     : Scheduler TokenCleanupTask.cleanupExpiredTokens START at=2026-11-25T14:05:40.029542900Z

1422: 2026-11-25T21:05:40.092+07:00  INFO 23220 --- [   scheduling-1] com.dormhub.service.TokenCleanupTask     : Scheduler TokenCleanupTask.cleanupExpiredTokens END at=2026-11-25T14:05:40.092995400Z

1423: 2026-11-25T21:05:50.000+07:00  INFO 23220 --- [   scheduling-1] com.dormhub.service.TokenCleanupTask     : Scheduler TokenCleanupTask.cleanupExpiredTokens START at=2026-11-25T14:05:50.000762200Z

1425: 2026-11-25T21:05:50.003+07:00  INFO 23220 --- [   scheduling-1] com.dormhub.service.TokenCleanupTask     : Scheduler TokenCleanupTask.cleanupExpiredTokens END at=2026-11-25T14:05:50.003802400Z

1426: 2026-11-25T21:06:00.002+07:00  INFO 23220 --- [   scheduling-1] com.dormhub.service.TokenCleanupTask     : Scheduler TokenCleanupTask.cleanupExpiredTokens START at=2026-11-25T14:06:00.002114Z

1428: 2026-11-25T21:06:00.005+07:00  INFO 23220 --- [   scheduling-1] com.dormhub.service.TokenCleanupTask     : Scheduler TokenCleanupTask.cleanupExpiredTokens END at=2026-11-25T14:06:00.005673400Z

```

### startup-false.log

```text

1418: 2026-11-25T21:06:31.131+07:00  INFO 24960 --- [           main] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup

```

### startup-true-1.log

```text

1419: 2026-11-25T21:07:02.489+07:00  INFO 11344 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth START at=2026-11-25T14:07:02.489328800Z

1420: 2026-11-25T21:07:02.816+07:00  INFO 11344 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-10, created=0, skipped=2, failed=0

1421: 2026-11-25T21:07:03.082+07:00  INFO 11344 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-11, created=6, skipped=16, failed=0

1422: 2026-11-25T21:07:03.083+07:00  INFO 11344 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth END at=2026-11-25T14:07:03.083702400Z

```

### startup-true-2.log

```text

1419: 2026-11-25T21:07:13.002+07:00  INFO 1996 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth START at=2026-11-25T14:07:13.002808800Z

1420: 2026-11-25T21:07:13.349+07:00  INFO 1996 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-10, created=0, skipped=2, failed=0

1421: 2026-11-25T21:07:13.457+07:00  INFO 1996 --- [   scheduling-1] c.dormhub.service.BillingBatchService    : ROOM_FEE billing complete: month=2026-11, created=0, skipped=22, failed=0

1422: 2026-11-25T21:07:13.458+07:00  INFO 1996 --- [   scheduling-1] com.dormhub.scheduler.BillingScheduler   : Scheduler BillingScheduler.reconcileCurrentMonth END at=2026-11-25T14:07:13.458377800Z

```

## Phân loại vấn đề và đề xuất

- Critical: chưa ghi nhận trong các ca đã thực thi.

- High: thiếu profile test trong source/package (ENV-PROFILE). Cần khôi phục và kiểm tra package trước khi dùng ngoài harness.

- Medium, giới hạn nghiệp vụ: nhắc hết hạn dedup theo type/refId; cùng contractId đã có thông báo sẽ không nhận nhắc mới sau gia hạn khi thông báo cũ còn tồn tại. Ca EX-renewed-reminder ghi nhận hành vi này, không coi là gửi nhắc cho kỳ mới thành công.

- Medium, giới hạn nghiệp vụ: không có Scheduler tự xử lý PENDING/HELD quá 7 ngày chưa chọn lịch; API từ chối chọn ngoài hạn nhưng assignment/occupancy vẫn giữ. Cần quy trình quản lý; chưa bật tịch thu cọc hoặc giải phóng tự động.

- Hết hạn hợp đồng chỉ đổi contracts.status; assignment CHECKED_IN và occupancy giữ nguyên đến Checkout. Xem ca EX-checkout-expired/complete để xác định luồng trả phòng có tiếp tục được trên hợp đồng EXPIRED.

- Low: Hibernate follow-on locking warning xuất hiện khi đối soát; chưa thấy ghi nhận trùng trong ca tuần tự 3 lượt. Chưa kiểm thử cạnh tranh nhiều instance.

## Giới hạn

Không kiểm thử chuyển tiền thật, email thật, frontend, concurrency đa instance hoặc production. Một số fixture đối chứng cố ý kết hợp trạng thái bất thường (ví dụ Payment SUCCESS với Invoice UNPAID) để kiểm tra bảo vệ không hủy; không coi đây là trạng thái bình thường của luồng nghiệp vụ.

Kết luận an toàn triển khai phải xem riêng lỗi profile; các ca runtime thành công không xóa bỏ lỗi đóng gói cấu hình. Các script/bằng chứng giữ lại để người dùng kiểm tra.

## Manifest dữ liệu giả lập được giữ lại

```json

[
  {
    "username": "qa-manager",
    "user_id": 1,
    "student_id": null,
    "contract_id": null,
    "room_id": null,
    "status": null,
    "deposit_status": null
  },
  {
    "username": "S01now",
    "user_id": 2,
    "student_id": 1,
    "contract_id": 1,
    "room_id": 1,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S01future",
    "user_id": 3,
    "student_id": 2,
    "contract_id": 2,
    "room_id": 2,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S01pending",
    "user_id": 4,
    "student_id": 3,
    "contract_id": 3,
    "room_id": 3,
    "status": "PENDING",
    "deposit_status": "HELD"
  },
  {
    "username": "S01expired",
    "user_id": 5,
    "student_id": 4,
    "contract_id": 4,
    "room_id": 4,
    "status": "EXPIRED",
    "deposit_status": "HELD"
  },
  {
    "username": "S01cancel",
    "user_id": 6,
    "student_id": 5,
    "contract_id": 5,
    "room_id": 5,
    "status": "CANCELLED",
    "deposit_status": "HELD"
  },
  {
    "username": "S01history",
    "user_id": 7,
    "student_id": 6,
    "contract_id": 6,
    "room_id": 6,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S01existing",
    "user_id": 8,
    "student_id": 7,
    "contract_id": 7,
    "room_id": 7,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S02now",
    "user_id": 9,
    "student_id": 8,
    "contract_id": 8,
    "room_id": 8,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S02future",
    "user_id": 10,
    "student_id": 9,
    "contract_id": 9,
    "room_id": 9,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S02pending",
    "user_id": 11,
    "student_id": 10,
    "contract_id": 10,
    "room_id": 10,
    "status": "PENDING",
    "deposit_status": "HELD"
  },
  {
    "username": "S02expired",
    "user_id": 12,
    "student_id": 11,
    "contract_id": 11,
    "room_id": 11,
    "status": "EXPIRED",
    "deposit_status": "HELD"
  },
  {
    "username": "S02cancel",
    "user_id": 13,
    "student_id": 12,
    "contract_id": 12,
    "room_id": 12,
    "status": "CANCELLED",
    "deposit_status": "HELD"
  },
  {
    "username": "S02history",
    "user_id": 14,
    "student_id": 13,
    "contract_id": 13,
    "room_id": 13,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S02existing",
    "user_id": 15,
    "student_id": 14,
    "contract_id": 14,
    "room_id": 14,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S03notdue",
    "user_id": 16,
    "student_id": 15,
    "contract_id": 15,
    "room_id": 15,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S03grace",
    "user_id": 17,
    "student_id": 16,
    "contract_id": 16,
    "room_id": 16,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S03overdue",
    "user_id": 18,
    "student_id": 17,
    "contract_id": 17,
    "room_id": 17,
    "status": "CANCELLED",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S03success",
    "user_id": 19,
    "student_id": 18,
    "contract_id": 18,
    "room_id": 18,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S03pending",
    "user_id": 20,
    "student_id": 19,
    "contract_id": 19,
    "room_id": 19,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S03unmatched",
    "user_id": 21,
    "student_id": 20,
    "contract_id": 20,
    "room_id": 20,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S03review",
    "user_id": 22,
    "student_id": 21,
    "contract_id": 21,
    "room_id": 21,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S04match",
    "user_id": 23,
    "student_id": 22,
    "contract_id": 22,
    "room_id": 22,
    "status": "PENDING",
    "deposit_status": "HELD"
  },
  {
    "username": "S04nopay",
    "user_id": 24,
    "student_id": 23,
    "contract_id": 23,
    "room_id": 23,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S04amount",
    "user_id": 25,
    "student_id": 24,
    "contract_id": 24,
    "room_id": 24,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S04late",
    "user_id": 26,
    "student_id": 25,
    "contract_id": 25,
    "room_id": 25,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S04matched",
    "user_id": 27,
    "student_id": 26,
    "contract_id": 26,
    "room_id": 26,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S04review",
    "user_id": 28,
    "student_id": 27,
    "contract_id": 27,
    "room_id": 27,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S04old",
    "user_id": 29,
    "student_id": 28,
    "contract_id": 28,
    "room_id": 28,
    "status": "PENDING",
    "deposit_status": "UNPAID"
  },
  {
    "username": "S05today",
    "user_id": 30,
    "student_id": 29,
    "contract_id": 29,
    "room_id": 29,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S05day3",
    "user_id": 31,
    "student_id": 30,
    "contract_id": 30,
    "room_id": 30,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S05day4",
    "user_id": 32,
    "student_id": 31,
    "contract_id": 31,
    "room_id": 31,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S05paid",
    "user_id": 33,
    "student_id": 32,
    "contract_id": 32,
    "room_id": 32,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S05cancel",
    "user_id": 34,
    "student_id": 33,
    "contract_id": 33,
    "room_id": 33,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S06yesterday",
    "user_id": 35,
    "student_id": 34,
    "contract_id": 34,
    "room_id": 34,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S06future",
    "user_id": 36,
    "student_id": 35,
    "contract_id": 35,
    "room_id": 35,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S06paid",
    "user_id": 37,
    "student_id": 36,
    "contract_id": 36,
    "room_id": 36,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S06todaypast",
    "user_id": 38,
    "student_id": 37,
    "contract_id": 37,
    "room_id": 37,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S06outside",
    "user_id": 39,
    "student_id": 38,
    "contract_id": 38,
    "room_id": 38,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S07soon",
    "user_id": 40,
    "student_id": 39,
    "contract_id": 39,
    "room_id": 39,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S07later",
    "user_id": 41,
    "student_id": 40,
    "contract_id": 40,
    "room_id": 40,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S07pending",
    "user_id": 42,
    "student_id": 41,
    "contract_id": 41,
    "room_id": 41,
    "status": "PENDING",
    "deposit_status": "HELD"
  },
  {
    "username": "S07cancel",
    "user_id": 43,
    "student_id": 42,
    "contract_id": 42,
    "room_id": 42,
    "status": "CANCELLED",
    "deposit_status": "HELD"
  },
  {
    "username": "S07existing",
    "user_id": 44,
    "student_id": 43,
    "contract_id": 43,
    "room_id": 43,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S07renewed",
    "user_id": 45,
    "student_id": 44,
    "contract_id": 44,
    "room_id": 44,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S08past",
    "user_id": 46,
    "student_id": 45,
    "contract_id": 45,
    "room_id": 45,
    "status": "EXPIRED",
    "deposit_status": "REFUNDED"
  },
  {
    "username": "S08future",
    "user_id": 47,
    "student_id": 46,
    "contract_id": 46,
    "room_id": 46,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "S08pending",
    "user_id": 48,
    "student_id": 47,
    "contract_id": 47,
    "room_id": 47,
    "status": "PENDING",
    "deposit_status": "HELD"
  },
  {
    "username": "S08expired",
    "user_id": 49,
    "student_id": 48,
    "contract_id": 48,
    "room_id": 48,
    "status": "EXPIRED",
    "deposit_status": "HELD"
  },
  {
    "username": "EX-startup",
    "user_id": 50,
    "student_id": 49,
    "contract_id": 49,
    "room_id": 49,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "EX-valid",
    "user_id": 51,
    "student_id": 50,
    "contract_id": 50,
    "room_id": 50,
    "status": "ACTIVE",
    "deposit_status": "HELD"
  },
  {
    "username": "EX-outside",
    "user_id": 52,
    "student_id": 51,
    "contract_id": 51,
    "room_id": 51,
    "status": "PENDING",
    "deposit_status": "HELD"
  },
  {
    "username": "EX-elapsed",
    "user_id": 53,
    "student_id": 52,
    "contract_id": 52,
    "room_id": 52,
    "status": "PENDING",
    "deposit_status": "HELD"
  }
]

```

## Lỗi harness đã khắc phục và chạy lại

Lượt đầu S09/S10 gặp NameError MANAGER trong script Python trước khi insert fixture hoặc bật Backend cho hai nhóm này. Đã sửa script bên ngoài source và chạy lại đầy đủ 3 lượt. Bằng chứng attempt ban đầu giữ tại initial-attempt-results.json; không quy lỗi này cho ứng dụng.

Lượt API đầu đăng nhập fixture thất bại do email tạo bằng SQL có chữ hoa, trong khi AuthService chuẩn hóa identifier thành chữ thường. Đã chuẩn hóa email fixture test; không sửa AuthService.

Hai thao tác API sau đó gặp 403 do harness giữ CSRF từ trước các request trung gian. Đã lấy lại CSRF qua endpoint hợp lệ ngay trước mutation, giữ nguyên CookieJar và cơ chế bảo mật; chạy lại thành công. Bằng chứng 403 giữ ở api-initial-attempt.json, log extra-api.log; lượt xác minh cuối ở api-fresh-csrf.log. Chưa kết luận đây là lỗi Scheduler hoặc phân quyền Backend.