package com.nhom7.coworkingspace.dto.request;

import org.springframework.web.multipart.MultipartFile;

import com.nhom7.coworkingspace.util.ValidImage;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class BecomeHostRequest {

    @Schema(description = "Business license file to upload. Optional if a license was already uploaded in a previous call.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @ValidImage(required = false)
    private MultipartFile businessLicense;
}
