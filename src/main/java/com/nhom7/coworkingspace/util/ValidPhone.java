package com.nhom7.coworkingspace.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Documented
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)

@NotBlank(message = "{validation.phone.required}")
@Pattern(regexp = "^(0)[35789][0-9]{8}$", message = "{validation.phone.invalid}")
public @interface ValidPhone {
  String message() default "{validation.phone.invalid}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
