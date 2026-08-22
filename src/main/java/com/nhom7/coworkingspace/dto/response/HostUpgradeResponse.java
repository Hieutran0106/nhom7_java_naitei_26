package com.nhom7.coworkingspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostUpgradeResponse {

    private UserProfileResponse profile;
    private boolean alreadyHost;
}
