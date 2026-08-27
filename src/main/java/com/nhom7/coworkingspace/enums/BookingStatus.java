package com.nhom7.coworkingspace.enums;

public enum BookingStatus {
    PENDING, // Chờ host xác nhận
    APPROVED,// Host đã xác nhận
    PAID, // User Đã thanh toán
    CONFIRMED, // Đã xác nhận 
    REJECTED, // Bị từ chối
    CANCELLED,// Đã hủy
    COMPLETED // Đã hoàn thành
}
