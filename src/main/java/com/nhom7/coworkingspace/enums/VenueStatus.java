package com.nhom7.coworkingspace.enums;

public enum VenueStatus {
     PENDING, //(đang chờ duyêt) venue mới được tạo -> chờ moderator/admin duyệt
     APPROVE,  // (đã được duyệt) -> có thể cho thuê
     BLOCKED  // (bị khóa): -> ghi thêm lý do -> user không thể booking vào venue này nữa

}
