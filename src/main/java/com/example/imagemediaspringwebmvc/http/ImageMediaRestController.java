package com.example.imagemediaspringwebmvc.http;

import com.example.imagemediaspringwebmvc.gcp.GcpDataUtil;
import com.example.imagemediaspringwebmvc.service.MediaStreamLoader;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/media-service")
@RequiredArgsConstructor
@Slf4j
public class ImageMediaRestController {

    private final MediaStreamLoader mediaLoaderService;
    private final GcpDataUtil gcpDataUtil;

    @GetMapping("/test-image")
    @SneakyThrows
    public ResponseEntity<byte[]> getTestImage(){
        ClassPathResource resource = new ClassPathResource("/WEB-INF/images/images.jpg");
        byte[] readFile = Files.readAllBytes(resource.getFile().toPath());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(readFile, httpHeaders, HttpStatus.OK);
    }

    @GetMapping(value = "/v01/play/{vid_id}")
    public ResponseEntity<StreamingResponseBody> playMediaV01(
            @PathVariable("vid_id")
            String video_id,
            @RequestHeader(value = "Range", required = false)
            String rangeHeader)
    {
        try
        {
            StreamingResponseBody responseStream;
            String filePathString = "C:\\Users\\avraa\\OneDrive\\Изображения\\Пленка\\WIN_20240618_10_33_35_Pro.mp4";
            Path filePath = Paths.get(filePathString);
            Long fileSize = Files.size(filePath);
            byte[] buffer = new byte[1024];
            final HttpHeaders responseHeaders = new HttpHeaders();

            if (rangeHeader == null)
            {
                responseHeaders.add("Content-Type", "video/mp4");
                responseHeaders.add("Content-Length", fileSize.toString());
                responseStream = os -> {
                    RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                    try (file)
                    {
                        long pos = 0;
                        file.seek(pos);
                        while (pos < fileSize - 1)
                        {
                            file.read(buffer);
                            os.write(buffer);
                            pos += buffer.length;
                        }
                        os.flush();
                    } catch (Exception e) {}
                };

                return new ResponseEntity<StreamingResponseBody>
                        (responseStream, responseHeaders, HttpStatus.OK);
            }

            String[] ranges = rangeHeader.split("-");
            Long rangeStart = Long.parseLong(ranges[0].substring(6));
            Long rangeEnd;
            if (ranges.length > 1)
            {
                rangeEnd = Long.parseLong(ranges[1]);
            }
            else
            {
                rangeEnd = fileSize - 1;
            }

            if (fileSize < rangeEnd)
            {
                rangeEnd = fileSize - 1;
            }

            String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
            responseHeaders.add("Content-Type", "video/mp4");
            responseHeaders.add("Content-Length", contentLength);
            responseHeaders.add("Accept-Ranges", "bytes");
            responseHeaders.add("Content-Range", "bytes" + " " +
                    rangeStart + "-" + rangeEnd + "/" + fileSize);
            final Long _rangeEnd = rangeEnd;
            responseStream = os -> {
                RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                try (file)
                {
                    long pos = rangeStart;
                    file.seek(pos);
                    while (pos < _rangeEnd)
                    {
                        file.read(buffer);
                        os.write(buffer);
                        pos += buffer.length;
                    }
                    os.flush();
                }
                catch (Exception e) {}
            };

            return new ResponseEntity<StreamingResponseBody>
                    (responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
        }
        catch (FileNotFoundException e)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catch (IOException e)
        {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping(value = "/v02/play/{vid_id}")
    public ResponseEntity<StreamingResponseBody> playMediaV02(
            @PathVariable("vid_id")
            String video_id,
            @RequestHeader(value = "Range", required = false)
            String rangeHeader)
    {
        try
        {
            String filePathString = "C:\\Users\\avraa\\OneDrive\\Изображения\\Пленка\\WIN_20240618_10_33_35_Pro.mp4";
            ResponseEntity<StreamingResponseBody> retVal =
                    mediaLoaderService.loadPartialMediaFile(filePathString, rangeHeader);

            return retVal;
        }
        catch (FileNotFoundException e)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catch (IOException e)
        {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



//    @GetMapping(value = "/v01/gcp/video")
//    public ResponseEntity<byte[]> playVideoFromGoogleStorageV01(
//            @RequestParam("fileName")
//            String fileName,
//            @RequestHeader(value = "Range", required = false)
//            String rangeHeader) {
//            return gcpDataUtil.getVideoFromBucket(fileName, rangeHeader);
//    }





    @GetMapping("/data")
    public ResponseEntity<?> saveNewObject(@RequestParam String key, @RequestParam String value){
        gcpDataUtil.saveNewObjectToBucket(key, value);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/object")
    public ResponseEntity<?> getObjectFromBucket(@RequestParam String key){
        String objectFromBucket = gcpDataUtil.getObjectFromBucket(key);
        return new ResponseEntity<>(objectFromBucket, HttpStatus.OK);
    }
    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveNewVideoInBucket(@RequestParam MultipartFile file){
        String fileName =  gcpDataUtil.saveNewVideoInBucket(file);
        return ResponseEntity.ok(fileName);
    }


//    @GetMapping(value = "/video")
//    public ResponseEntity<?> fetchVideoFromBucketAndSaveInProject(@RequestParam String key) throws IOException {
//        gcpDataUtil.streamObjectDownload("social-media-app-160800",
//                "n1nt3nd0-bucket-russo",
//                key);
//        log.info("Stream object download complete successfully!");
//        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
    @GetMapping(value = "/video-content")
    public ResponseEntity<byte[]> getVideoFromBucketAndReturnToClient(@RequestParam String key) throws IOException {
       gcpDataUtil.streamObjectDownloadAndReturnToClient("social-media-app-160800",
                "n1nt3nd0-bucket-russo",
                key);
        log.info("Stream object download IN <RestController> complete successfully!");
//        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.setContentType(MediaType.valueOf("video/mp4"));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/stream-video")
    public ResponseEntity<StreamingResponseBody> streamVideoFromGoogleCloud(@RequestParam("fileName")String fileName) {
        return gcpDataUtil.streamVideoFromGcp(fileName);
    }
}
