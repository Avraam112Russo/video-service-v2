package com.example.imagemediaspringwebmvc.gcp;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

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
}
