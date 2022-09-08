package com.example.demo2.services;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AwsS3Service {
    void uploadFile(MultipartFile file,String fileName);
    List<String> getObjectsFromS3();
}
