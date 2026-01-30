package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.MediaDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.MediaBaseService;
import com.lektralabs.thrones.pallbearer.media.pipeline.realsports.ChallengeMediaStore;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class MediaService extends MediaBaseService {
    private static final Logger logger = Logger.getLogger(MediaService.class);

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    ChallengeMediaStore challengeMediaStore;

    @Inject
    UserService userService;

    private MediaDao mediaDao;

    @PostConstruct
    public void init() {
        super.init();
        this.mediaDao = jdbiProvider.getJdbi().onDemand(MediaDao.class);
    }

    public int setMediaStatus(UUID mediaId, String mediaStatusCode) {
        return mediaDao.updateMediaStatus(mediaId, mediaStatusCode);
    }

    /**
     * Converts Google Drive file URLs to direct download URLs for all media items.
     * Converts from: https://drive.google.com/file/d/{id}/view (with optional query params)
     * To: https://drive.google.com/uc?export=download&id={id}
     * 
     * @return ConversionResult containing count of converted items and any errors
     */
    @Transactional
    public ConversionResult convertGoogleDriveContentUrls() {
        logger.info("Starting Google Drive content URL conversion");
        
        List<MediaDao.MediaContentUrlRow> itemsToConvert = mediaDao.findAllWithGoogleDriveContentUrls();
        logger.info("Found " + itemsToConvert.size() + " media items with Google Drive content URLs");
        
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
        
        for (MediaDao.MediaContentUrlRow item : itemsToConvert) {
            try {
                String originalUrl = item.getContentUrl();
                
                // Skip if already converted
                if (originalUrl != null && originalUrl.startsWith("https://drive.google.com/uc?export=download&id=")) {
                    logger.debug("Skipping already converted content URL for media " + item.getId());
                    continue;
                }
                
                Matcher matcher = pattern.matcher(originalUrl);
                
                if (matcher.matches()) {
                    String fileId = matcher.group(1);
                    String convertedUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
                    
                    int updated = mediaDao.updateContentUrl(
                            item.getId(),
                            convertedUrl,
                            currentTime
                    );
                    
                    if (updated > 0) {
                        convertedCount++;
                        logger.debug("Converted content URL for media " + item.getId() + 
                                   ": " + originalUrl + " -> " + convertedUrl);
                    } else {
                        errorCount++;
                        errors.add("Failed to update media " + item.getId());
                    }
                } else {
                    errorCount++;
                    errors.add("URL pattern mismatch for media " + item.getId() + ": " + originalUrl);
                }
            } catch (Exception e) {
                errorCount++;
                String errorMsg = "Error converting content URL for media " + item.getId() + ": " + e.getMessage();
                errors.add(errorMsg);
                logger.error(errorMsg, e);
            }
        }
        
        logger.info("Conversion completed: " + convertedCount + " converted, " + errorCount + " errors");
        
        return new ConversionResult(convertedCount, errorCount, errors);
    }

    /**
     * Result class for content URL conversion operation
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
