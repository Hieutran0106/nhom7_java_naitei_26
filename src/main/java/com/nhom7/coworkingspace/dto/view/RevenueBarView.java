package com.nhom7.coworkingspace.dto.view;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RevenueBarView {

    private int month;
    private BigDecimal revenue;
    private int percentage;
}
