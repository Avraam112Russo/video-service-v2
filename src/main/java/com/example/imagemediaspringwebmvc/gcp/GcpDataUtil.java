package com.example.imagemediaspringwebmvc.gcp;

import com.example.imagemediaspringwebmvc.service.MediaStreamLoader;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GcpDataUtil {

    private final Bucket bucket;
    private final MediaStreamLoader mediaStreamLoader;
    private final Storage storage;

    public void saveNewObjectToBucket(String key, String value) {
        try {

            byte[] valueAsBytes = value.getBytes(StandardCharsets.UTF_8);
            Blob blob = bucket.create(key, valueAsBytes);
            log.info("Object saved successfully: {}", blob.toString());
        } catch (Exception e) {
            log.error("Error while saveNewObjectToBucket IN GcpDataUtil: {}", e.getMessage());
            throw new RuntimeException("Error while saveNewObjectToBucket IN GcpDataUtil");
        }
    }

    public String getObjectFromBucket(String blobId) {
        try {

            Blob blob = bucket.get(blobId);
            return new String(blob.getContent());
        } catch (Exception e) {
            log.error("Error while getObjectFromBucket IN GcpDataUtil: {}", e.getMessage());
            throw new RuntimeException("Error while getObjectFromBucket IN GcpDataUtil");
        }
    }

    @SneakyThrows
    public String saveNewVideoInBucket(MultipartFile multipartFile) {
        try {
            byte[] videoFilesAsBytesArray = FileUtils.readFileToByteArray(converted(multipartFile));
            String fileName = UUID.randomUUID().toString();
            ;
            Blob savedVideo = bucket.create(fileName, videoFilesAsBytesArray);
            log.info("Video file saved successfully: {}", savedVideo.toString());
            return fileName;
        } catch (Exception e) {
            log.error("An error has occurred while saveNewVideoInBucket IN GcpDataUtil: {}", e.getMessage());
            throw new RuntimeException("An error has occurred while saveNewVideoInBucket IN GcpDataUtil");
        }
    }


//    @SneakyThrows
//    public ResponseEntity<byte[]>  getVideoFromBucket(String fileName){
//        try {
//
//            Blob blob = bucket.get(fileName);
//            ReadChannel readChannel = blob.reader();
//            String filePath = "C:\\Users\\avraa\\IdeaProjects\\image-media-spring-webmvc\\src\\main\\resources\\WEB-INF\\images\\" + fileName + ".mp4";
//            File file = new File(filePath + fileName + ".mp4");
//            FileOutputStream fileOutputStream = new FileOutputStream(file);
//            fileOutputStream.getChannel().transferFrom(readChannel, 0, Long.MAX_VALUE);
//            fileOutputStream.close();
//            byte[] bytes = FileUtils.readFileToByteArray(file);
//            return new ResponseEntity<>(bytes, HttpStatus.OK);
//        }catch (Exception e){
//            log.error("Error while getVideoFromBucket IN GcpDataUtil: {}", e.getMessage());
//            throw new RuntimeException("Error while getVideoFromBucket IN GcpDataUtil");
//        }
//    }


    public void streamObjectDownload(
            String projectId, String bucketName, String objectName) throws IOException {
        Path targetFilePath = Paths.get("newVideoFileGcp.mp4");
        Path file = Files.createFile(targetFilePath);

        try (ReadChannel reader = storage.reader(BlobId.of(bucketName, objectName));
             FileChannel targetFileChannel =
                     FileChannel.open(targetFilePath, StandardOpenOption.WRITE)) {

            ByteStreams.copy(reader, targetFileChannel);

            System.out.println(
                    "Downloaded object "
                            + objectName
                            + " from bucket "
                            + bucketName
                            + " to "
                            + file
                            + " using a ReadChannel.");
        }
    }

    public void streamObjectDownloadAndReturnToClient(
            String projectId, String bucketName, String objectName) throws IOException {
        Path targetFilePath = Paths.get("newVideoFileGcp.mp4");
        Path file = Files.createFile(targetFilePath);

        BlobId blobId = BlobId.of(bucketName, objectName);

        try (ReadChannel reader = storage.reader(blobId);
             FileChannel targetFileChannel =
                     FileChannel.open(targetFilePath, StandardOpenOption.WRITE)) {

            ByteStreams.copy(reader, targetFileChannel);
            System.out.println(
                    "Downloaded object "
                            + objectName
                            + " from bucket "
                            + bucketName
                            + " to "
                            + file
                            + " using a ReadChannel.");
        }

    }


    private File converted(MultipartFile multipartFile) {

        try {
            if (multipartFile.getOriginalFilename() == null) {
                throw new BadRequestException("Original file name is null");
            }
            File convertedFile = new File(multipartFile.getOriginalFilename());
            FileOutputStream fileOutputStream = new FileOutputStream(convertedFile);
            fileOutputStream.write(multipartFile.getBytes());
            fileOutputStream.close();
            log.info("Converting multipart file: {}", convertedFile);
            return convertedFile;
        } catch (Exception e) {
            throw new RuntimeException("An error has occurred while converting the file");
        }
    }

    public ResponseEntity<StreamingResponseBody> streamVideoFromGcp(String fileName) {

        Blob blob = storage.get(BlobId.of(bucket.getName(), fileName));
        if (blob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        StreamingResponseBody responseBody = outputStream -> {
            try (ReadChannel reader = blob.reader()) {
                byte[] buffer = new byte[1024];
                int limit;
                while ((limit = reader.read(ByteBuffer.wrap(buffer))) >= 0) {
                    outputStream.write(buffer, 0, limit);
                }
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(responseBody);
    }
}



