package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;


public interface UserService {

   
    UpdateUserRoleResponse addRole(Long userId, String roleName);
}