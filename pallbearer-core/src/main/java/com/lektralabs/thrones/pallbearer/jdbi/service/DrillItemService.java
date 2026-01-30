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

    public List<List<DrillItemDetail>> getAllDrills() {
        Map<String, List<DrillItemDetail>> groupedDrills = getAllDrillItemsGroupedByDrillGroup();
        return groupedDrills.values().stream()
                .collect(Collectors.toList());
    }

    public Map<String, List<DrillItemDetail>> getAllDrillItemsGroupedByDrillGroup() {
        List<DrillItemDetail> allDrillItems = drillItemDao.findAll();
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

