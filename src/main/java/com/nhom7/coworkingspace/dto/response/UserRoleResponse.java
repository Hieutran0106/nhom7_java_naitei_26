package com.nhom7.coworkingspace.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * Dữ liệu trả về sau khi thêm role thành công.
 */
@Getter
@Builder
public class UserRoleResponse {

    private Long id;

    private String name;

    private String email;

  
    private Set<String> roles;
}