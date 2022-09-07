package com.example.demo2.controller;

import com.example.demo2.entities.MultipartImage;
import com.example.demo2.services.AwsS3Service;
import com.example.demo2.utils.FingerReader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.zkteco.biometric.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/test")
public class TestController {

    private boolean mbStop = true;
    private byte[] template = new byte[2048];
    private long mhDevice = 0;
    private long mhDB = 0;
    byte[] imgbuf;
    private int nFakeFunOn = 1;

    private final FingerReader fingerReader;
    private final AwsS3Service aws;
    public TestController(FingerReader fingerReader, AwsS3Service aws) {
        this.fingerReader = fingerReader;
        this.aws = aws;
    }

    @GetMapping()
    @ResponseStatus(value = HttpStatus.OK)
    public String getMethodName() throws IOException, InterruptedException {
        FingerprintSensorEx.Init();
        long devHandle = FingerprintSensorEx.OpenDevice(0);
        byte[] paramValue = new byte[4];
        int[] size = new int[1];

        size[0] = 4;
        FingerprintSensorEx.GetParameters(devHandle, 1, paramValue, size);
        int fpWidth = fingerReader.byteArrayToInt(paramValue);

        size[0] = 4;
        FingerprintSensorEx.GetParameters(devHandle, 2, paramValue, size);
        int fpHeight = fingerReader.byteArrayToInt(paramValue);

        imgbuf = new byte[fpWidth * fpHeight];

        size[0] = 2048;
        while (true){
            int response = FingerprintSensorEx.AcquireFingerprint(devHandle, imgbuf, template, size);
            System.out.println(response);
            Thread.sleep(2000L);
            if (response == 0) {
                fingerReader.writeBitmap(imgbuf, fpWidth, fpHeight, "src/main/resources/images/fingerprint.bmp");
                File input = new File("src/main/resources/images/fingerprint.bmp");
                BufferedImage image = ImageIO.read(input);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos );
                baos.flush();
                MultipartFile multipartFile = new MultipartImage(baos.toByteArray(),"fingerprint.jpg","fingerprint.jpg", MediaType.MULTIPART_FORM_DATA.toString(), baos.size());
                aws.uploadFile(multipartFile);
                break;
            }
        }
        return "testing";
    }
}
