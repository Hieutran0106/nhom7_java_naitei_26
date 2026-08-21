package com.nhom7.coworkingspace.enums;

import java.util.Locale;

public enum PriceUnit {
    HOUR,
    DAY,
    MONTH;

    public static PriceUnit fromString(String unit) {
        if (unit == null || unit.isBlank()) {
            return HOUR;
        }
        try {
            return PriceUnit.valueOf(unit.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return HOUR;
        }
    }
}
