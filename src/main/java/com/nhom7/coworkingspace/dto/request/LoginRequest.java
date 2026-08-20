package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.util.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @ValidEmail
  @NotBlank(message = "{validation.email.required}")
  private String email;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "{validation.password.required}")
  private String password;
}
