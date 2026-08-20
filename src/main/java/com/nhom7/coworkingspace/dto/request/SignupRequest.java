package com.nhom7.coworkingspace.dto.request;

import org.springframework.web.multipart.MultipartFile;

import com.nhom7.coworkingspace.util.ValidEmail;
import com.nhom7.coworkingspace.util.ValidImage;
import com.nhom7.coworkingspace.util.ValidPassword;
import com.nhom7.coworkingspace.util.ValidPhone;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class SignupRequest {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "{validation.name.required}")
  @Size(max = 150, message = "{validation.name.size}")
  private String name;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @ValidEmail
  private String email;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @ValidPassword
  private String password;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @ValidPhone
  private String phone;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @ValidImage
  private MultipartFile cccdImage;
}
