package com.lektralabs.thrones.pallbearer.jdbi.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class GoogleDriveDownloadService {

    private static final Logger logger = Logger.getLogger(GoogleDriveDownloadService.class);

    @ConfigProperty(name = "pallbearer.media.store")
    String mediaStorePath;

    // Pattern to match Google Drive file URLs
    // Matches: https://drive.google.com/file/d/{fileId}/view (with optional query params)
    private static final Pattern GOOGLE_DRIVE_FILE_PATTERN = Pattern.compile(
            "https://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)"
    );

    /**
     * Converts Google Drive share URL to direct download URL
     * 
     * @param shareUrl Google Drive share URL (e.g., https://drive.google.com/file/d/{id}/view?usp=sharing)
     * @return Direct download URL (e.g., https://drive.google.com/uc?export=download&id={id})
     */
    public String convertToDirectDownloadUrl(String shareUrl) {
        if (shareUrl == null || shareUrl.isEmpty()) {
            return null;
        }

        // If already a direct download URL, return as is
        if (shareUrl.contains("uc?export=download")) {
            return shareUrl;
        }

        Matcher matcher = GOOGLE_DRIVE_FILE_PATTERN.matcher(shareUrl);
        if (matcher.find()) {
            String fileId = matcher.group(1);
            return "https://drive.google.com/uc?export=download&id=" + fileId;
        }

        logger.warn("Could not extract file ID from Google Drive URL: " + shareUrl);
        return shareUrl; // Return original URL if pattern doesn't match
    }

    /**
     * Converts Google Drive share URL to direct view URL (for thumbnails/images)
     * 
     * @param shareUrl Google Drive share URL
     * @return Direct view URL (e.g., https://drive.google.com/uc?export=view&id={id})
     */
    public String convertToDirectViewUrl(String shareUrl) {
        if (shareUrl == null || shareUrl.isEmpty()) {
            return null;
        }

        // If already a direct view URL, return as is
        if (shareUrl.contains("uc?export=view")) {
            return shareUrl;
        }

        Matcher matcher = GOOGLE_DRIVE_FILE_PATTERN.matcher(shareUrl);
        if (matcher.find()) {
            String fileId = matcher.group(1);
            return "https://drive.google.com/uc?export=view&id=" + fileId;
        }

        logger.warn("Could not extract file ID from Google Drive URL: " + shareUrl);
        return shareUrl;
    }

    /**
     * Downloads a file from a URL to a local path
     * 
     * @param fileUrl URL to download from
     * @param localFilePath Local file path to save to
     * @return true if download succeeded, false otherwise
     */
    public boolean downloadFile(String fileUrl, String localFilePath) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            logger.warn("Cannot download file: URL is null or empty");
            return false;
        }

        try {
            // Create parent directories if they don't exist
            Path filePath = Paths.get(localFilePath);
            Files.createDirectories(filePath.getParent());

            // Check if file already exists and has content - skip download if so
            if (Files.exists(filePath) && Files.size(filePath) > 0) {
                logger.info("File already exists and has content, skipping download: " + localFilePath + 
                           " (size: " + Files.size(filePath) + " bytes)");
                return true; // Return true since file exists and is valid
            }

            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30 seconds
            connection.setReadTimeout(60000); // 60 seconds
            
            // Set user agent to avoid blocking
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(localFilePath)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }
                    
                    logger.info("Successfully downloaded file from " + fileUrl + " to " + localFilePath + 
                               " (" + totalBytesRead + " bytes)");
                    return true;
                }
            } else {
                logger.error("Failed to download file from " + fileUrl + ": HTTP " + responseCode);
                return false;
            }
        } catch (IOException e) {
            logger.error("Error downloading file from " + fileUrl + " to " + localFilePath + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Downloads a video file from Google Drive
     * 
     * @param googleDriveUrl Google Drive share URL
     * @param localFilePath Local file path to save to
     * @return true if download succeeded, false otherwise
     */
    public boolean downloadVideo(String googleDriveUrl, String localFilePath) {
        String directUrl = convertToDirectDownloadUrl(googleDriveUrl);
        return downloadFile(directUrl, localFilePath);
    }

    /**
     * Downloads a thumbnail/image file from Google Drive
     * 
     * @param googleDriveUrl Google Drive share URL
     * @param localFilePath Local file path to save to
     * @return true if download succeeded, false otherwise
     */
    public boolean downloadThumbnail(String googleDriveUrl, String localFilePath) {
        String directUrl = convertToDirectViewUrl(googleDriveUrl);
        return downloadFile(directUrl, localFilePath);
    }

    /**
     * Sanitizes a string to be used as a folder or file name
     * Removes or replaces invalid characters
     * 
     * @param name Original name
     * @return Sanitized name safe for use in file system
     */
    public String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        
        // Replace invalid characters with underscore
        String sanitized = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Remove multiple consecutive underscores
        sanitized = sanitized.replaceAll("_{2,}", "_");
        
        // Remove leading/trailing underscores and dots
        sanitized = sanitized.replaceAll("^[._]+|[._]+$", "");
        
        // Limit length to avoid filesystem issues
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    /**
     * Builds the local file path for a media file based on group, level, and drill name
     * 
     * @param groupName Drill group name (e.g., "Beginner")
     * @param levelIndex Level index
     * @param drillName Drill name
     * @param mediaId Media ID (UUID)
     * @param fileName File name (e.g., "video.mp4" or "thumbnail.jpg")
     * @return Full local file path
     */
    public String buildMediaFilePath(String groupName, Integer levelIndex, String drillName, String mediaId, String fileName) {
        String sanitizedGroup = sanitizeFileName(groupName != null ? groupName : "Unknown");
        String sanitizedDrill = sanitizeFileName(drillName != null ? drillName : "Unknown");
        String levelPrefix = levelIndex != null ? levelIndex + "_" : "";
        
        String folderPath = String.format("%s/gallery/%s/%s%s/%s",
                mediaStorePath,
                sanitizedGroup,
                levelPrefix,
                sanitizedDrill,
                mediaId);
        
        return folderPath + "/" + fileName;
    }
}
