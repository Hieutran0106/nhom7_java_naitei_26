package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.UserRoleResponse;


public interface UserService {

   
    UserRoleResponse addRole(Long userId, String roleName);
}