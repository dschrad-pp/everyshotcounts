package com.lektralabs.thrones.pallbearer.jdbi.service;

import java.util.LinkedHashMap;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillItemPartial;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillItemDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.DrillItemBaseService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Import Optional
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow; // Import DrillGroupRow

@ApplicationScoped
public class DrillItemService extends DrillItemBaseService {

    private static final Logger logger = Logger.getLogger(DrillItemService.class);

    private DrillItemDao drillItemDao;

    @PostConstruct
    public void init() {
        super.init();
        this.drillItemDao = jdbiProvider.getJdbi().onDemand(DrillItemDao.class);
    }

    @Transactional
    public UUID createWithOrder(DrillItemPartial drillItemPartial) {
        int drillItemOrder = drillItemDao.findMaxItemOrder(drillItemPartial.getDrillGroupId(), drillItemPartial.getLevelIndex());
        return super.create(drillItemPartial.toBuilder().drillItemOrder(drillItemOrder + 1).build());
    }

    public int updateMediaId(UUID drillItemId, UUID mediaId) {
        return drillItemDao.updateMediaId(drillItemId, mediaId);
    }

    @Transactional
    public int updateMediaThumbnail(UUID drillItemId, String mediaThumbnail) {
        return drillItemDao.updateMediaThumbnail(drillItemId, mediaThumbnail, System.currentTimeMillis());
    }

    public List<List<DrillItemDetail>> getAllDrills() {
        Map<String, List<DrillItemDetail>> groupedDrills = getAllDrillItemsGroupedByDrillGroup();
        return groupedDrills.values().stream()
                .collect(Collectors.toList());
    }

    public Map<String, List<DrillItemDetail>> getAllDrillItemsGroupedByDrillGroup() {
        List<DrillItemDetail> allDrillItems = drillItemDao.findAll();
        
        // Convert local mediaThumbnail paths to URLs
        allDrillItems.forEach(drillItem -> {
            if (drillItem.getMediaThumbnail().isPresent()) {
                String thumbnailPath = drillItem.getMediaThumbnail().get();
                // Check if it's a local file path (starts with /)
                if (thumbnailPath != null && thumbnailPath.startsWith("/") && !thumbnailPath.startsWith("http")) {
                    // Convert local path to URL
                    String thumbnailUrl = convertLocalPathToUrl(thumbnailPath);
                    logger.debug("Converted local thumbnail path to URL: " + thumbnailPath + " -> " + thumbnailUrl);
                    drillItem.setMediaThumbnail(Optional.of(thumbnailUrl));
                }
                // If it's already a URL (http/https), leave it as is
            }
        });
        
        Map<String, List<DrillItemDetail>> groupedDrills = allDrillItems.stream()
                .collect(Collectors.groupingBy(
                        drillItem -> Optional.ofNullable(drillItem.getDrillGroup()) // Wrap in Optional
                                .flatMap(DrillGroupRow::getName) // Use flatMap to get Optional<String>
                                .orElse("Unknown Group"), // Provide a default if DrillGroup is null or name is empty
                        LinkedHashMap::new, // Use LinkedHashMap to maintain order of groups
                        Collectors.toList()
                ));

        return groupedDrills;
    }
    
    /**
     * Converts a local file path to a URL that can be accessed via HTTP.
     * Supports both old format (with /gallery/) and new format (directly under mediaStorePath).
     * 
     * Old format: {mediaStorePath}/gallery/{group}/{level}_{drill}/{mediaId}/thumbnail.jpg
     * New format: {mediaStorePath}/{groupName}/{unique_id}/thumbnail.jpg
     * 
     * @param localPath Local file path (e.g., "/home/ankit/Downloads/thrones-development/media/Beginner/beginners_1_1/thumbnail.jpg")
     * @return URL that can be used to access the file
     */
    private String convertLocalPathToUrl(String localPath) {
        if (localPath == null || localPath.isEmpty()) {
            return null;
        }
        
        // If it's already a URL (http/https), return as-is
        if (localPath.startsWith("http://") || localPath.startsWith("https://")) {
            return localPath;
        }
        
        // Try to extract relative path from the local path
        String relativePath = null;
        
        // Check for new format: {mediaStorePath}/{groupName}/{unique_id}/thumbnail.jpg
        // This format doesn't have "/gallery/" in it
        if (localPath.contains("/thumbnail.jpg") && !localPath.contains("/gallery/")) {
            // Check if path matches new format: ends with /thumbnail.jpg and has structure {group}/{unique_id}/thumbnail.jpg
            String[] pathParts = localPath.split("/");
            if (pathParts.length >= 3 && pathParts[pathParts.length - 1].equals("thumbnail.jpg")) {
                // Find the directories before thumbnail.jpg
                // Last part is "thumbnail.jpg", second last is {unique_id}, third last is {groupName}
                String uniqueId = pathParts[pathParts.length - 2];
                String groupName = pathParts.length >= 3 ? pathParts[pathParts.length - 3] : null;
                
                // Check if this looks like the new format (groupName/unique_id/thumbnail.jpg)
                // Common group names: Beginner, Intermediate, Advanced, Elite (case-insensitive)
                if (groupName != null) {
                    String[] knownGroups = {"Beginner", "Intermediate", "Advanced", "Elite", 
                                           "beginner", "intermediate", "advanced", "elite"};
                    
                    for (String group : knownGroups) {
                        if (groupName.equalsIgnoreCase(group)) {
                            // Found a group name, extract relative path: {groupName}/{unique_id}/thumbnail.jpg
                            relativePath = groupName + "/" + uniqueId + "/thumbnail.jpg";
                            logger.debug("Detected new format path - Group: " + groupName + ", UniqueId: " + uniqueId + ", Relative path: " + relativePath);
                            break;
                        }
                    }
                }
            }
        }
        
        // If we didn't find new format, try old format with /gallery/
        if (relativePath == null) {
            int galleryIndex = localPath.indexOf("/gallery/");
            if (galleryIndex >= 0) {
                // Extract relative path starting from "gallery/"
                relativePath = localPath.substring(galleryIndex + 1); // +1 to skip the leading /
            }
        }
        
        // If we have a relative path (either new or old format), use query parameter endpoint
        if (relativePath != null) {
            try {
                String encodedPath = java.net.URLEncoder.encode(relativePath, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20");
                String url = String.format("http://103.99.202.227:8000/api/media/gallery/thumbnail?path=%s", encodedPath);
                logger.debug("Converted local path " + localPath + " to URL: " + url);
                return url;
            } catch (Exception e) {
                logger.warn("Error encoding path: " + relativePath, e);
                return String.format("http://103.99.202.227:8000/api/media/gallery/thumbnail?path=%s", relativePath);
            }
        }
        
        // Fallback: if we can't extract relative path, try to use the full path
        logger.warn("Could not extract relative path from: " + localPath);
        try {
            String encodedPath = java.net.URLEncoder.encode(localPath, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return String.format("http://103.99.202.227:8000/api/media/gallery/thumbnail?path=%s", encodedPath);
        } catch (Exception e) {
            return localPath; // Return original path if we can't convert it
        }
    }

    /**
     * Converts Google Drive file URLs to direct view URLs for all drill items.
     * Converts from: https://drive.google.com/file/d/{id}/view (with optional query params)
     * To: https://drive.google.com/uc?export=view&id={id}
     * 
     * @return ConversionResult containing count of converted items and any errors
     */
    @Transactional
    public ConversionResult convertGoogleDriveThumbnails() {
        logger.info("Starting Google Drive thumbnail conversion");
        
        List<DrillItemDao.DrillItemThumbnailRow> itemsToConvert = drillItemDao.findAllWithGoogleDriveThumbnails();
        logger.info("Found " + itemsToConvert.size() + " drill items with Google Drive thumbnails");
        
        int convertedCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        // Pattern to match: https://drive.google.com/file/d/{id}/view (with optional query params)
        // Matches URLs like:
        // - https://drive.google.com/file/d/{id}/view
        // - https://drive.google.com/file/d/{id}/view?usp=drive_link
        // - https://drive.google.com/file/d/{id}/view?usp=sharing
        // - https://drive.google.com/file/d/{id}/view?usp=drive_web
        Pattern pattern = Pattern.compile("^https://drive\\.google\\.com/file/d/([^/]+)/view(\\?.*)?$");
        
        for (DrillItemDao.DrillItemThumbnailRow item : itemsToConvert) {
            try {
                String originalUrl = item.getMediaThumbnail();
                
                // Skip if already converted
                if (originalUrl != null && originalUrl.startsWith("https://drive.google.com/uc?export=view&id=")) {
                    logger.debug("Skipping already converted thumbnail for drill item " + item.getId());
                    continue;
                }
                
                Matcher matcher = pattern.matcher(originalUrl);
                
                if (matcher.matches()) {
                    String fileId = matcher.group(1);
                    String convertedUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
                    
                    int updated = drillItemDao.updateMediaThumbnail(
                            item.getId(),
                            convertedUrl,
                            currentTime
                    );
                    
                    if (updated > 0) {
                        convertedCount++;
                        logger.debug("Converted thumbnail for drill item " + item.getId() + 
                                   ": " + originalUrl + " -> " + convertedUrl);
                    } else {
                        errorCount++;
                        errors.add("Failed to update drill item " + item.getId());
                    }
                } else {
                    errorCount++;
                    errors.add("URL pattern mismatch for drill item " + item.getId() + ": " + originalUrl);
                }
            } catch (Exception e) {
                errorCount++;
                String errorMsg = "Error converting thumbnail for drill item " + item.getId() + ": " + e.getMessage();
                errors.add(errorMsg);
                logger.error(errorMsg, e);
            }
        }
        
        logger.info("Conversion completed: " + convertedCount + " converted, " + errorCount + " errors");
        
        return new ConversionResult(convertedCount, errorCount, errors);
    }

    /**
     * Result class for thumbnail conversion operation
     */
    public static class ConversionResult {
        private int convertedCount;
        private int errorCount;
        private List<String> errors;

        public ConversionResult(int convertedCount, int errorCount, List<String> errors) {
            this.convertedCount = convertedCount;
            this.errorCount = errorCount;
            this.errors = errors;
        }

        public int getConvertedCount() {
            return convertedCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
