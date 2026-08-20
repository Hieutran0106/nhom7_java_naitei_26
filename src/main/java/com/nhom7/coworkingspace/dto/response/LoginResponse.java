package com.nhom7.coworkingspace.dto.response;

import com.nhom7.coworkingspace.enums.UserStatus;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

  private String accessToken;
  private String refreshToken;

  @Builder.Default
  private String tokenType = "Bearer";

  private Long id;
  private String name;
  private String email;
  private String phone;
  private UserStatus status;
  private Boolean isIdentityVerified;
  private String language;
  private Set<String> roles;
}
