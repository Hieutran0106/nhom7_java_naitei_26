package com.nhom7.coworkingspace.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
// Not blank, min 8 characters, contain lowercase, uppercase, number, special character and no whitespace
@NotBlank(message = "{validation.password.required}")
@Size(min = 8, message = "{validation.password.size}")
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?`~\\\\])[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?`~\\\\]+$", message = "{validation.password.pattern}")
public @interface ValidPassword {
    String message() default "{validation.password.pattern}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
