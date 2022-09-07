package com.example.dependency.services;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AwsS3Service {
    void uploadFile(MultipartFile file);
    List<String> getObjectsFromS3();
}
