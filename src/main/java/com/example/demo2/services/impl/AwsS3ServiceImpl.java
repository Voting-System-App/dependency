package com.example.demo2.services.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.example.demo2.services.AwsS3Service;
import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintImageOptions;
import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AwsS3ServiceImpl implements AwsS3Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsS3ServiceImpl.class);
    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public AwsS3ServiceImpl(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    @Override
    public void uploadFile(MultipartFile file,String fileName) {
        File mainFile = new File(file.getOriginalFilename());
        try (FileOutputStream stream = new FileOutputStream(mainFile)) {
            stream.write(file.getBytes());
            String newFileName = fileName;
            PutObjectRequest request = new PutObjectRequest(bucketName, newFileName, mainFile);
            amazonS3.putObject(request);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    @Override
    public List<String> getObjectsFromS3() {
        ListObjectsV2Result result = amazonS3.listObjectsV2(bucketName);
        List<S3ObjectSummary> objects = result.getObjectSummaries();
        List<String> list = objects.stream().map(item -> item.getKey()).collect(Collectors.toList());
        return list;
    }

    @Override
    public Boolean validate(String fileName) throws IOException {
        S3Object object = amazonS3.getObject(bucketName,fileName);
        Path path = Paths.get("src/main/resources/images/" + fileName + ".jpg");
        byte[] s3Object = IOUtils.toByteArray(object.getObjectContent());
        byte[] byteArray = Files.readAllBytes(path);
        FingerprintTemplate probe = new FingerprintTemplate(
                new FingerprintImage(
                        s3Object,
                        new FingerprintImageOptions()
                                .dpi(500)));
        FingerprintTemplate candidate = new FingerprintTemplate(
                new FingerprintImage(
                       byteArray,
                        new FingerprintImageOptions()
                                .dpi(500)));
        double score = new FingerprintMatcher(probe)
                .match(candidate);
        double threshold = 40;
        boolean matches = score >= threshold;
        return matches;
    }
}
