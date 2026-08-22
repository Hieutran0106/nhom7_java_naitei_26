package com.nhom7.coworkingspace.dto.request;

import org.springframework.web.multipart.MultipartFile;

import com.nhom7.coworkingspace.util.ValidImage;
import com.nhom7.coworkingspace.util.ValidPhone;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class UpdateUserRequest {

  @Schema(description = "Full name. Omit to keep the current value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @Size(max = 150, message = "{validation.name.size}")
  private String name;

  @Schema(description = "Phone number. Omit to keep the current value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @ValidPhone
  private String phone;

  @Schema(description = "New CCCD image to replace the current one. Omit to keep the current value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @ValidImage(required = false)
  private MultipartFile cccdImage;
}
