package com.example.imagemediaspringwebmvc.gcp;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GcpDataUtil {

    private final Bucket bucket;

    public void saveNewObjectToBucket(String key,String value){
        try {

        byte[] valueAsBytes = value.getBytes(StandardCharsets.UTF_8);
        Blob blob = bucket.create(key, valueAsBytes);
        log.info("Object saved successfully: {}", blob.toString());
        }catch (Exception e){
            log.error("Error while saveNewObjectToBucket IN GcpDataUtil: {}", e.getMessage());
            throw new RuntimeException("Error while saveNewObjectToBucket IN GcpDataUtil");
        }
    }
    public String getObjectFromBucket(String blobId){
        try {

        Blob blob = bucket.get(blobId);
        return new String(blob.getContent());
        }catch (Exception e){
            log.error("Error while getObjectFromBucket IN GcpDataUtil: {}", e.getMessage());
            throw new RuntimeException("Error while getObjectFromBucket IN GcpDataUtil");
        }
    }

    @SneakyThrows
    public String saveNewVideoInBucket(MultipartFile multipartFile){
        try {
        byte [] videoFilesAsBytesArray = FileUtils.readFileToByteArray(converted(multipartFile));
        String fileName = UUID.randomUUID().toString();;
        Blob savedVideo = bucket.create(fileName, videoFilesAsBytesArray);
        log.info("Video file saved successfully: {}", savedVideo.toString());
        return fileName;
        }catch (Exception e){
            log.error("An error has occurred while saveNewVideoInBucket IN GcpDataUtil: {}", e.getMessage());
            throw new RuntimeException("An error has occurred while saveNewVideoInBucket IN GcpDataUtil");
        }
    }

    private File converted(MultipartFile multipartFile) {

        try {
            if (multipartFile.getOriginalFilename() == null){
                throw new BadRequestException("Original file name is null");
            }
            File convertedFile = new File(multipartFile.getOriginalFilename());
            FileOutputStream fileOutputStream = new FileOutputStream(convertedFile);
            fileOutputStream.write(multipartFile.getBytes());
            fileOutputStream.close();
            log.info("Converting multipart file: {}", convertedFile);
            return convertedFile;
        }catch (Exception e){
            throw new RuntimeException("An error has occurred while converting the file");
        }
    }


}
