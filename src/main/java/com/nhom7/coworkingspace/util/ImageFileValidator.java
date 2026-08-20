package com.nhom7.coworkingspace.util;

import java.util.List;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class ImageFileValidator implements ConstraintValidator<ValidImage, MultipartFile> {

  private long maxSize;
  private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
      "image/jpeg",
      "image/png",
      "image/webp");

  @Override
  public void initialize(ValidImage constraintAnnotation) {
    this.maxSize = constraintAnnotation.maxSizeInBytes();
  }

  @Override
  public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
    // Check if file is empty
    if (file == null || file.isEmpty()) {
      buildConstraintViolation(context, "{validation.image.required}");
      return false;
    }

    // Check file size
    if (file.getSize() > maxSize) {
      buildConstraintViolation(context, "{validation.image.size}");
      return false;
    }

    // Check file content type
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
      buildConstraintViolation(context, "{validation.image.invalid}");
      return false;
    }

    return true;
  }

  private void buildConstraintViolation(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
  }
}
