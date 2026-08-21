package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.dto.response.UserProfileResponse;


public interface UserService {


    UpdateUserRoleResponse addRole(Long userId, String roleName);

    UserProfileResponse getMyProfile(String email);
}