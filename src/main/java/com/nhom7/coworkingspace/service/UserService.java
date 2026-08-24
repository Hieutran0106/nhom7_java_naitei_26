package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.UpdateUserRequest;
import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.dto.response.HostUpgradeResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserStatusResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserVerificationResponse;
import com.nhom7.coworkingspace.dto.response.UserProfileResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.enums.UserStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    UpdateUserRoleResponse addRole(Long userId, String roleName);

    UpdateUserRoleResponse removeRole(Long userId, String roleName);

    UserProfileResponse getMyProfile(String email);

    UserProfileResponse updateMyProfile(String email, UpdateUserRequest request);

    /**
     * Upgrade the currently authenticated USER to the HOST role, once both identity and
     * business verification have been approved. The business license file is optional -
     * it may already have been uploaded (and possibly verified) in a previous call.
     *
     * @param email           email of the authenticated user (from SecurityContext)
     * @param businessLicense optional business license file to upload/replace
     * @return the resulting profile, plus whether the user already had the HOST role
     */
    HostUpgradeResponse becomeHost(String email, MultipartFile businessLicense);

    /**
     * Search/filter/paginate users, scoped to what the caller is allowed to see.
     * A MODERATOR caller (without the ADMIN role) never sees ADMIN accounts in the results.
     *
     * @param request           search parameters
     * @param currentUserEmail  email of the authenticated caller (from SecurityContext)
     */
    PageResponse<UserSearchResponse> searchUsers(UserSearchRequest request, String currentUserEmail);

    UserSearchResponse getUserById(Long userId);

    List<String> getAvailableRoleNames();

    UpdateUserStatusResponse updateUserStatus(
            Long targetUserId,
            UserStatus newStatus,
            String currentAdminEmail
    );

    UpdateUserVerificationResponse updateIdentityVerification(
            Long targetUserId,
            boolean verified,
            String currentAdminEmail
    );

    UpdateUserVerificationResponse updateBusinessVerification(
            Long targetUserId,
            boolean verified,
            String currentAdminEmail
    );
}