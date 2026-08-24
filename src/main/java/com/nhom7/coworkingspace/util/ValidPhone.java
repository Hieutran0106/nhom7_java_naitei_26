package com.nhom7.coworkingspace.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

// Intentionally does not stack @NotBlank: this annotation is reused on both required fields
// (signup, paired with an explicit @NotBlank there) and optional partial-update fields (where
// null must stay valid so the field can be omitted). @Pattern alone already treats null as
// valid and still enforces the format whenever a value is present.
@Documented
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)

@Pattern(regexp = "^(0)[35789][0-9]{8}$", message = "{validation.phone.invalid}")
public @interface ValidPhone {
  String message() default "{validation.phone.invalid}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
