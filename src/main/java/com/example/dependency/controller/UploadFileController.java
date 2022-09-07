package com.example.dependency.controller;

import com.example.dependency.services.AwsS3Service;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/s3")
@Slf4j
public class UploadFileController {
    @Value("${aws.s3.bucket}")
    private String name;

    private final AwsS3Service aws;

    public UploadFileController(AwsS3Service aws) {
        this.aws = aws;
    }

    @PostMapping(value = "/upload")
    public ResponseEntity<String> uploadFile(@RequestPart(value="file") MultipartFile file) {
        aws.uploadFile(file);
        String response = "El archivo "+file.getOriginalFilename()+" fue cargado correctamente a S3";
        return new ResponseEntity<String>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/list")
    public ResponseEntity<List<String>> listFiles() {
        return new ResponseEntity<List<String>>(aws.getObjectsFromS3(), HttpStatus.OK);
    }
}
