package com.dormhub.entity.enums;

public enum NotificationType {
    // 1. Bảo trì (Phạm vi của bạn)
    MAINTENANCE_STATUS_CHANGED,

    // 2. Đăng ký & Phân phòng (Phạm vi của TV2)
    APPLICATION_SUBMITTED,
    APPLICATION_APPROVED,
    APPLICATION_REJECTED,
    ROOM_ASSIGNED,

    // 3. Hợp đồng & Cư trú (Phạm vi của TV3)
    CONTRACT_CREATED,
    CONTRACT_ACTIVATED,
    CHECKIN_REMINDER,
    RENEWAL_PROCESSED,
    TRANSFER_PROCESSED,
    CHECKOUT_CONFIRMED,

    // 4. Hóa đơn & Thanh toán (Phạm vi của TV4)
    INVOICE_CREATED,
    INVOICE_DUE_REMINDER,
    INVOICE_OVERDUE,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,

    // 6. Hợp đồng sắp hết hạn
    CONTRACT_EXPIRING,

    // 5. Thông báo chung
    GENERAL,
    MANAGER_ANNOUNCEMENT
}
