package com.nhom7.coworkingspace.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceCreateRequest {

    @NotBlank(message = "{validation.space.name.required}")
    @Size(max = 200, message = "{validation.space.name.size}")
    private String name;

    @Size(max = 50, message = "{validation.space.type.size}")
    private String type;

    @NotNull(message = "{validation.space.capacity.required}")
    @Min(value = 1, message = "{validation.space.capacity.min}")
    private Integer capacity;

    private String description;

    @NotNull(message = "{validation.space.price.required}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{validation.space.price.min}")
    private BigDecimal price;

    @NotBlank(message = "{validation.space.priceUnit.required}")
    private String priceUnit;

    @NotNull(message = "{validation.space.openTime.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime openTime;

    @NotNull(message = "{validation.space.closeTime.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime closeTime;
}
