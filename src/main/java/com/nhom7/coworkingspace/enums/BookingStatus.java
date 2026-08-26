package com.nhom7.coworkingspace.enums;

public enum BookingStatus {
    PENDING, //(đang chờ duyệt) booking mới được tạo -> chờ host duyệt
    APPROVED, //(đã được duyệt) -> host đã duyệt booking này -> user có thể thanh toán
    CONFIRMED, //(đã xác nhận) -> user đã thanh toán -> host có thể check-in user vào space
    REJECTED, //(đã bị từ chối) -> host đã từ chối booking này
    CANCELLED, //(đã bị hủy) -> user đã hủy booking này
    COMPLETED // (đã hoàn thành) -> booking đã hoàn tất
}
