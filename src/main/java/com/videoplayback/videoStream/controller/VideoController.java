package com.videoplayback.videoStream.controller;

import com.videoplayback.videoStream.Entity.Video;
import com.videoplayback.videoStream.Entity.VideoStatus;
import com.videoplayback.videoStream.Payload.CustomMessage;
import com.videoplayback.videoStream.Service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static java.util.Base64.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin("*")
public class VideoController {


    @Autowired
    private VideoService videoService;
    @Value("${files.video}")
    private String DIR;
    @Value("${files.video.hsl}")
    private String HSL_DIR;
    @Value("${files.thumbnail}")
    String THUMBNAIL_DIR;


    @PostMapping("/videos/add")
    public ResponseEntity<?> createVideo(@RequestParam ("file") MultipartFile file,
                                         @RequestParam("title") String title,
                                         @RequestParam("description") String description,
                                         @RequestParam("thumbnail") MultipartFile thumbnail)
     {



        try {

            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setVideoId(UUID.randomUUID().toString());
            video.setThumbnail(thumbnail.toString());

            Video savedVideo = videoService.save(video, file,thumbnail);
            if (savedVideo != null) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(savedVideo);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(CustomMessage.builder()
                                .message("Failed to save video")
                                .success(false)
                                .build()
                        );
            }
        } catch (Exception e) {
            // Log the exception with a message
            System.err.println("Error occurred while creating video: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomMessage.builder()
                            .message("An unexpected error occurred")
                            .success(false)
                            .build()
                    );
        }
    }
    @GetMapping("/videos")
    public List<Video> getAll() {
        List<Video> videos = videoService.getAll();
        for (Video video : videos) {

            video.setThumbnail(video.getThumbnail());
        }
        return videos;
    }

    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String videoId) {
        Video video = videoService.get(videoId);
        String contentType=video.getContentType();
        String filePath = video.getFilePath();
        if(contentType ==null){
            contentType="application/octet-stream";
        }
        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    //Send Videos In Required bytes


    @GetMapping("/stream/range/{videoId}")
    public ResponseEntity<Resource> streamVideoRange(@PathVariable String videoId,
                                                     @RequestHeader(value = "Range", required = false) String range) {


        Video video = videoService.get(videoId);
        Path path = Paths.get(video.getFilePath());

        Resource resource = new FileSystemResource(path);
        String contentType = video.getContentType() != null ? video.getContentType() : "application/octet-stream";
        long fileLength = path.toFile().length();
        System.out.println("Range Header: " + range+"-"+fileLength);

        if (range == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }

        long rangeStart;
        long rangeEnd;

        try {
            String[] ranges = range.replace("bytes=", "").split("-");
            rangeStart = Long.parseLong(ranges[0]);
            rangeEnd = ranges.length > 1 ? Long.parseLong(ranges[1]) : rangeStart + AppConstants.CHUNK_SIZE - 1;
            if (rangeEnd >= fileLength) {
                rangeEnd = fileLength - 1;
            }

            System.out.println("Range Start: " + rangeStart);
            System.out.println("Range End: " + rangeEnd);

            InputStream inputStream = Files.newInputStream(path);
            inputStream.skip(rangeStart);
            byte[] data = new byte[(int) (rangeEnd - rangeStart + 1)];
            int bytesRead = inputStream.read(data);
            inputStream.close();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("X-Content-Type-Options", "nosniff");
            headers.setContentLength(bytesRead);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(data));

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/stream/{videoId}/master.m3u8")
    public ResponseEntity<Resource> getMasterFile(@PathVariable String videoId){

        Path path = Paths.get(HSL_DIR,videoId,"master.m3u8");
        System.out.println(path);
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        Resource resource = new FileSystemResource(path);
        return  ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")//prevention for no cache
                .body(resource);
    }
    @GetMapping("/stream/{videoId}/{segment}.ts")
    public ResponseEntity<Resource> getSegmentFile(@PathVariable String videoId
    ,@PathVariable String segment){
        Path path = Paths.get(HSL_DIR,videoId,segment+".ts");
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        Resource resource = new FileSystemResource(path);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE,"video/mp2t")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);

    }

    @GetMapping("/{videoId}")
    public ResponseEntity<Video> getVideo(@PathVariable String videoId) {
        Video video = videoService.get(videoId);
        return ResponseEntity.ok(video);
    }
    @GetMapping("/thumbnails/{filename}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        try {

            Path thumbnailDirectory = Paths.get(THUMBNAIL_DIR);

            // Resolve the full path to the thumbnail
            Path filePath = thumbnailDirectory.resolve(filename);

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while trying to fetch thumbnail", e);
        }
    }

}
