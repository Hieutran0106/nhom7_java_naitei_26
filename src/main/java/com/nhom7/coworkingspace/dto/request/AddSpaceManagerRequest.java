package com.nhom7.coworkingspace.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddSpaceManagerRequest {

    @NotNull(message = "{validation.space.manager.id.required}")
    private Long userId;
}
