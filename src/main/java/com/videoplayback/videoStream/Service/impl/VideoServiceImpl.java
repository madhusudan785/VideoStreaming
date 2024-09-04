package com.videoplayback.videoStream.Service.impl;

import com.videoplayback.videoStream.Entity.Video;
import com.videoplayback.videoStream.Entity.VideoStatus;
import com.videoplayback.videoStream.Reposetory.VideoRepo;
import com.videoplayback.videoStream.Service.VideoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import static com.videoplayback.videoStream.Payload.RandomStringGenerator.generateRandomString;

@Service
public class VideoServiceImpl implements VideoService {
    @Autowired
    private VideoRepo videoRepo;

    public VideoServiceImpl(VideoRepo videoRepo) {
        this.videoRepo = videoRepo;
    }

    @Value("${files.video}")
    String DIR;

    @Value("${files.video.hsl}")
    String HSL_DIR;
    @Value("${files.thumbnail}")
    String THUMBNAIL_DIR;

    @PostConstruct
    public void init() {
        createDirectories(DIR);
        createDirectories(HSL_DIR);
        createDirectories(THUMBNAIL_DIR);
    }

    private void createDirectories(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("Directory created: " + dirPath);
            } else {
                System.err.println("Failed to create directory: " + dirPath);
            }
        } else {
            System.out.println("Directory already exists: " + dirPath);
        }
    }


    @Override
    public Video save(Video video, MultipartFile file,MultipartFile thumbnail) {

        //original fileName
        try {
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            String cleanFileName = StringUtils.cleanPath(fileName);
            String cleanFolder = StringUtils.cleanPath(DIR);
            Path videoPath = Paths.get(cleanFolder, cleanFileName);
            Files.copy(inputStream, videoPath, StandardCopyOption.REPLACE_EXISTING);

            String thumbnailFileName = StringUtils.cleanPath(thumbnail.getOriginalFilename());
            InputStream thumbnailInputStream = thumbnail.getInputStream(); // This is where the `thumbnailInputStream` comes from

            // Define the thumbnail save path
            String uniqueThumbnailFileName = UUID.randomUUID() + ".JPEG";
            Path thumbnailPath = Paths.get("thumbnails", uniqueThumbnailFileName);
            Files.copy(thumbnailInputStream, thumbnailPath, StandardCopyOption.REPLACE_EXISTING);

            // Set video details
            video.setContentType(contentType);
            video.setFilePath(videoPath.toString());
            video.setThumbnail(thumbnailPath.toString()); // Relative path for the thumbnail

            // Save video entity
            videoRepo.save(video);

            // Start video processing asynchronously
            processVideo(video.getVideoId());


            return video;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Video get(String videoId) {
        return videoRepo.findById(videoId).orElseThrow(() -> new RuntimeException("Video not found"));
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoRepo.findAll();
    }


    @Override
    @Async
    public Future<String> processVideo(String videoId) {
        Video video = this.get(videoId);
        String filePath = video.getFilePath();

        // Path where to store data:
        Path path = Paths.get(filePath);

        try {
            Path outputPath = Paths.get(HSL_DIR, videoId);
            Files.createDirectories(outputPath);

            // Print paths for debugging
            System.out.println("Input file path: " + path);
            System.out.println("Output directory path: " + outputPath);

            // FFmpeg command
            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\" \"%s/master.m3u8\"",
                    path.toString(), outputPath.toString(), outputPath.toString()
            );


            System.out.println("Executing FFmpeg command: " + ffmpegCmd);

            // Use ProcessBuilder to run the command
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                video.setStatus(VideoStatus.FAILED);
                videoRepo.save(video);
                throw new RuntimeException("Video processing failed!!");
            }
            video.setStatus(VideoStatus.COMPLETED);
            videoRepo.save(video);

            return new AsyncResult<>(videoId);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            video.setStatus(VideoStatus.FAILED);
            videoRepo.save(video);
            throw new RuntimeException("Failed to start video processing", e);
        }
    }

    @Override
    public boolean isProcessingComplete(String videoId) {
        Video video = videoRepo.findById(videoId).orElseThrow(() -> new RuntimeException("Video not found"));
        return video.getStatus() == VideoStatus.COMPLETED || video.getStatus() == VideoStatus.FAILED;
    }


}

