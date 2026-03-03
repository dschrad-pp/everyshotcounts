package com.lektralabs.thrones.pallbearer.jdbi.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Service for generating video thumbnails/still frames using ffmpeg
 */
@ApplicationScoped
public class ThumbnailGenerationService {

    private static final Logger logger = Logger.getLogger(ThumbnailGenerationService.class);

    @ConfigProperty(name = "pallbearer.media.ffmpeg.path")
    String ffmpegPath;

    /**
     * Generate a thumbnail/still frame from a video file
     * 
     * @param videoFile Input video file
     * @param outputThumbnailPath Output path for the thumbnail image
     * @param timeOffset Time offset in seconds (default: 2 seconds)
     * @return true if thumbnail was generated successfully, false otherwise
     */
    public boolean generateThumbnail(File videoFile, String outputThumbnailPath, int timeOffset) {
        if (videoFile == null || !videoFile.exists()) {
            logger.errorf("Video file does not exist: %s", videoFile);
            return false;
        }

        File thumbnailFile = new File(outputThumbnailPath);
        File thumbnailDir = thumbnailFile.getParentFile();
        
        // Create parent directory if it doesn't exist
        if (thumbnailDir != null && !thumbnailDir.exists()) {
            try {
                Files.createDirectories(thumbnailDir.toPath());
                logger.infof("Created thumbnail directory: %s", thumbnailDir.getAbsolutePath());
            } catch (IOException e) {
                logger.errorf(e, "Failed to create thumbnail directory: %s", thumbnailDir.getAbsolutePath());
                return false;
            }
        }

        // Build ffmpeg command: ffmpeg -nostdin -y -i input.mp4 -ss 00:00:02 -frames:v 1 -q:v 2 still-frame.jpg
        ProcessBuilder processBuilder = new ProcessBuilder(
            ffmpegPath,
            "-nostdin",
            "-y",
            "-i", videoFile.getAbsolutePath(),
            "-ss", String.format("00:00:%02d", timeOffset),
            "-frames:v", "1",
            "-q:v", "2",
            outputThumbnailPath
        );

        processBuilder.redirectErrorStream(true);
        
        try {
            logger.infof("Generating thumbnail for video: %s -> %s", videoFile.getAbsolutePath(), outputThumbnailPath);
            
            Process process = processBuilder.start();
            
            // Wait for process to complete with timeout (60 seconds)
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            
            if (!finished) {
                logger.errorf("Thumbnail generation timed out for video: %s", videoFile.getAbsolutePath());
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode == 0 && thumbnailFile.exists() && thumbnailFile.length() > 0) {
                logger.infof("Successfully generated thumbnail: %s (%d bytes)", outputThumbnailPath, thumbnailFile.length());
                return true;
            } else {
                logger.errorf("Failed to generate thumbnail. Exit code: %d, File exists: %s, File size: %d", 
                    exitCode, thumbnailFile.exists(), thumbnailFile.exists() ? thumbnailFile.length() : 0L);
                return false;
            }
            
        } catch (IOException e) {
            logger.errorf(e, "IO error while generating thumbnail for video: %s", videoFile.getAbsolutePath());
            return false;
        } catch (InterruptedException e) {
            logger.errorf(e, "Thumbnail generation interrupted for video: %s", videoFile.getAbsolutePath());
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.errorf(e, "Unexpected error while generating thumbnail for video: %s", videoFile.getAbsolutePath());
            return false;
        }
    }

    /**
     * Generate a thumbnail/still frame from a video file using default 2 second offset
     * 
     * @param videoFile Input video file
     * @param outputThumbnailPath Output path for the thumbnail image
     * @return true if thumbnail was generated successfully, false otherwise
     */
    public boolean generateThumbnail(File videoFile, String outputThumbnailPath) {
        return generateThumbnail(videoFile, outputThumbnailPath, 2);
    }
}
