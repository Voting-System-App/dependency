package com.example.demo2.controller;

import com.example.demo2.entities.MultipartImage;
import com.example.demo2.services.AwsS3Service;
import com.example.demo2.utils.FingerReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.zkteco.biometric.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
@RestController
@RequestMapping("/s3")
public class ReaderController {
    private byte[] template = new byte[2048];
    byte[] imgbuf;

    @Value("${temp.images}")
    private String temp;
    private final FingerReader fingerReader;
    private final AwsS3Service aws;
    public ReaderController(FingerReader fingerReader, AwsS3Service aws) {
        this.fingerReader = fingerReader;
        this.aws = aws;
    }

    @GetMapping("/{fileName}/validate/{flag}")
    @ResponseStatus(value = HttpStatus.OK)
    public Boolean getMethodName(@PathVariable String fileName,@PathVariable Boolean flag) throws IOException, InterruptedException {
        Boolean var = false;
        int cont = 4;
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
            Thread.sleep(3000L);
            cont--;
            if(cont==0){
                break;
            }
            if (response == 0) {
                if(flag==false){
                    fingerReader.writeBitmap(imgbuf, fpWidth, fpHeight,temp+fileName+".bmp");
                    File input = new File(temp+fileName+".bmp");
                    BufferedImage image = ImageIO.read(input);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos );
                    baos.flush();
                    MultipartFile multipartFile = new MultipartImage(baos.toByteArray(),fileName+".jpg",fileName+".jpg", MediaType.MULTIPART_FORM_DATA.toString(), baos.size());
                    aws.uploadFile(multipartFile,fileName);
                    var = true;
                    break;
                }
                else {
                    fingerReader.writeBitmap(imgbuf, fpWidth, fpHeight, temp+fileName+".bmp");
                    File input = new File(temp+fileName+".bmp");
                    BufferedImage image = ImageIO.read(input);
                    File output = new File(temp+fileName+".jpg");
                    ImageIO.write(image, "jpg", output);
                    var = aws.validate(fileName);
                    break;
                }
            }
        }
        return var;
    }
    @DeleteMapping("/buffer/{fileName}")
    public String deleteBuffer(@PathVariable String fileName) throws IOException {
        Path pathBmp = Paths.get(temp + fileName + ".bmp");
        Path pathTemp = Paths.get( fileName + ".jpg");
        Files.delete(pathBmp);
        Files.deleteIfExists(pathTemp);
        return "buffer limpiado";
    }
    @PostMapping(value = "/upload/{dni}")
    public ResponseEntity<String> uploadFile(@RequestPart(value="file") MultipartFile file,@PathVariable String dni) {
        aws.uploadFileProfile(file,dni);
        String response = "El archivo "+dni+" fue cargado correctamente a S3";
        return new ResponseEntity<String>(response, HttpStatus.OK);
    }
}
