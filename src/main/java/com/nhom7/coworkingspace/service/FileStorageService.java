package com.nhom7.coworkingspace.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeFile(MultipartFile file, String subDirectory);

    String createSignedUrl(String filePath, int expiresInSeconds);
}
