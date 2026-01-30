package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillItemImportDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.MediaImportDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillItemDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.MediaDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemImportRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.MediaImportRow;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DrillItemImportService {

    private static final Logger logger = Logger.getLogger(DrillItemImportService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    @Inject
    protected GoogleDriveDownloadService googleDriveDownloadService;

    @ConfigProperty(name = "pallbearer.media.store")
    String mediaStorePath;

    private DrillItemImportDao drillItemImportDao;
    private MediaImportDao mediaImportDao;
    private DrillItemDao drillItemDao;
    private MediaDao mediaDao;

    @PostConstruct
    public void init() {
        this.drillItemImportDao = jdbiProvider.getJdbi().onDemand(DrillItemImportDao.class);
        this.mediaImportDao = jdbiProvider.getJdbi().onDemand(MediaImportDao.class);
        this.drillItemDao = jdbiProvider.getJdbi().onDemand(DrillItemDao.class);
        this.mediaDao = jdbiProvider.getJdbi().onDemand(MediaDao.class);
    }

    @Transactional
    public int importFromCsv(InputStream csvInputStream) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
                CurrentUser auditUser = userService.getCurrentUser();
                long currentTime = System.currentTimeMillis();
                UUID defaultUserId = auditUser != null ? auditUser.getId() : UUID.fromString("d79ab826-65de-4fda-8b5f-779dacfe00fe");
                
            List<DrillItemImportRow> rows;
            try {
                rows = parseCsv(csvInputStream, auditUser);
            } catch (Exception e) {
                logger.error("Error parsing CSV file", e);
                throw new TransactionException(e);
            }
                
                if (rows.isEmpty()) {
                    logger.warn("No rows to import from CSV");
                    return 0;
                }

            int successCount = 0;
            int updateCount = 0;
            int insertCount = 0;
            int failureCount = 0;
            List<String> failureDetails = new ArrayList<>();

            // Process each row: create media records if needed, then insert or update drill item
            for (int i = 0; i < rows.size(); i++) {
                DrillItemImportRow row = rows.get(i);
                int rowNumber = i + 2; // +2 because CSV rows are 1-indexed and we skip header
                
                try {
                    // If media_id (content_url) is provided, create/find media import record
                    if (row.getMediaId().isPresent() && !row.getMediaId().get().isEmpty()) {
                        String contentUrl = row.getMediaId().get();
                        UUID mediaId = getOrCreateMediaImport(contentUrl, row.getName(), row.getDescription(), currentTime, defaultUserId);
                        row.setMediaId(Optional.of(mediaId.toString()));
                    }
                    
                    // Check if unique_id is provided and not empty
                    boolean shouldUpdate = false;
                    DrillItemImportRow existingRow = null;
                    
                    if (row.getUniqueId().isPresent() && row.getUniqueId().get() != null && !row.getUniqueId().get().trim().isEmpty()) {
                        String uniqueId = row.getUniqueId().get().trim();
                        Optional<DrillItemImportRow> existing = drillItemImportDao.findByUniqueId(uniqueId);
                        
                        if (existing.isPresent()) {
                            shouldUpdate = true;
                            existingRow = existing.get();
                            logger.debug("Found existing drill item with unique_id: " + uniqueId + " - will update instead of insert");
                        }
                    }
                    
                    if (shouldUpdate && existingRow != null) {
                        // Update existing row with new values
                        // Preserve the original ID, creation_date, and created_by_id
                        row.setId(existingRow.getId());
                        row.setCreationDate(existingRow.getCreationDate());
                        row.setCreatedById(existingRow.getCreatedById());
                        row.setVersion(existingRow.getVersion());
                        row.setModificationDate(currentTime);
                        row.setModifiedById(defaultUserId);
                        
                        int updated = drillItemImportDao.update(row);
                        if (updated > 0) {
                            updateCount++;
                            successCount++;
                            logger.debug("Updated drill item - Row " + rowNumber + 
                                       ": unique_id=" + row.getUniqueId().orElse("null") +
                                       ", drill_group_id=" + row.getDrillGroupId() + 
                                       ", level_index=" + row.getLevelIndex());
                        } else {
                            throw new RuntimeException("Update failed - no rows affected. Version conflict or row not found.");
                        }
                    } else {
                        // Insert new row
                    drillItemImportDao.insert(row);
                        insertCount++;
                        successCount++;
                        
                        // Log detailed info for debugging level 19 and 20
                        if (row.getLevelIndex() != null && (row.getLevelIndex() == 19 || row.getLevelIndex() == 20)) {
                            logger.debug("Successfully imported drill item - Row " + rowNumber + 
                                       ": drill_group_id=" + row.getDrillGroupId() + 
                                       ", level_index=" + row.getLevelIndex() + 
                                       ", drill_item_order=" + row.getDrillItemOrder() +
                                       ", name=" + row.getName().orElse("null") +
                                       ", unique_id=" + row.getUniqueId().orElse("null"));
                        }
                    }
            } catch (Exception e) {
                    failureCount++;
                    String errorMsg = "Row " + rowNumber + " failed: " + e.getMessage();
                    failureDetails.add(errorMsg);
                    
                    // Log detailed error info for level 19 and 20
                    if (row.getLevelIndex() != null && (row.getLevelIndex() == 19 || row.getLevelIndex() == 20)) {
                        logger.error("Failed to import drill item - Row " + rowNumber + 
                                   ": drill_group_id=" + row.getDrillGroupId() + 
                                   ", level_index=" + row.getLevelIndex() + 
                                   ", drill_item_order=" + row.getDrillItemOrder() +
                                   ", name=" + row.getName().orElse("null") +
                                   ", unique_id=" + row.getUniqueId().orElse("null") +
                                   ", error=" + e.getMessage(), e);
                    } else {
                        logger.warn(errorMsg);
                    }
                    // Continue processing other rows instead of failing entire import
                }
            }
            
            if (failureCount > 0) {
                logger.warn("Import completed with " + failureCount + " failures out of " + rows.size() + " total rows. " +
                           "Successfully processed: " + successCount + " rows (" + insertCount + " inserted, " + updateCount + " updated). " +
                           "Failed rows: " + String.join("; ", failureDetails));
            } else {
                logger.info("Successfully processed all " + successCount + " rows from CSV (" + insertCount + " inserted, " + updateCount + " updated)");
            }
            
            return successCount;
        });
    }

    private List<DrillItemImportRow> parseCsv(InputStream csvInputStream, CurrentUser auditUser) throws Exception {
        List<DrillItemImportRow> rows = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        UUID defaultUserId = auditUser != null ? auditUser.getId() : UUID.fromString("d79ab826-65de-4fda-8b5f-779dacfe00fe");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvInputStream, StandardCharsets.UTF_8))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            int lineNumber = 1; // Start at 1 since we already read the header
            int skippedRows = 0;
            List<String> skippedRowDetails = new ArrayList<>();
            
            // Read complete CSV rows (handling multi-line quoted fields)
            String completeRow = readCompleteCsvRow(reader);
            int startLineNumber = lineNumber + 1;
            
            while (completeRow != null) {
                lineNumber++;
                
                completeRow = completeRow.trim();
                if (completeRow.isEmpty()) {
                    completeRow = readCompleteCsvRow(reader);
                    continue; // Skip empty rows
                }

                try {
                    DrillItemImportRow row = parseCsvLine(completeRow, startLineNumber, currentTime, defaultUserId);
                    rows.add(row);
                } catch (Exception e) {
                    skippedRows++;
                    String errorDetail = "Row starting at line " + startLineNumber + ": " + e.getMessage();
                    skippedRowDetails.add(errorDetail);
                    
                    // Log detailed error for debugging
                    logger.warn("Error parsing CSV row starting at line " + startLineNumber + ": " + e.getMessage() + 
                              ". Row content (first 200 chars): " + 
                              (completeRow.length() > 200 ? completeRow.substring(0, 200) + "..." : completeRow));
                    // Continue processing other rows
                }
                
                // Read next complete row
                completeRow = readCompleteCsvRow(reader);
                startLineNumber = lineNumber + 1;
            }
            
            if (skippedRows > 0) {
                logger.warn("Skipped " + skippedRows + " rows during CSV parsing. Details: " + 
                           String.join("; ", skippedRowDetails));
            }
        }

        return rows;
    }

    /**
     * Reads a complete CSV row, handling multi-line quoted fields.
     * A row is complete when all opening quotes have matching closing quotes.
     * 
     * @param reader BufferedReader to read from
     * @return Complete CSV row as a single string, or null if EOF
     */
    private String readCompleteCsvRow(BufferedReader reader) throws Exception {
        StringBuilder rowBuilder = new StringBuilder();
        String line = reader.readLine();
        
        if (line == null) {
            return null; // EOF
        }
        
        rowBuilder.append(line);
        
        // Count quotes to determine if we have a complete row
        // A complete row has an even number of quotes (all quotes are properly closed)
        int quoteCount = countQuotes(rowBuilder.toString());
        
        // If we have an odd number of quotes, the row spans multiple lines
        // Continue reading until we have an even number of quotes
        while (quoteCount % 2 != 0) {
            line = reader.readLine();
            if (line == null) {
                // EOF reached but quotes are not closed - this is an error, but we'll return what we have
                logger.warn("CSV row has unclosed quotes. Row content: " + 
                           (rowBuilder.length() > 200 ? rowBuilder.substring(0, 200) + "..." : rowBuilder.toString()));
                break;
            }
            rowBuilder.append("\n").append(line);
            quoteCount = countQuotes(rowBuilder.toString());
        }
        
        return rowBuilder.toString();
    }

    /**
     * Counts the number of unescaped quote delimiters in a string.
     * Handles escaped quotes (double quotes "" inside a quoted field).
     * Returns the count of actual quote delimiters (not escaped quotes).
     */
    private int countQuotes(String text) {
        int count = 0;
        boolean inQuotes = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '"') {
                // Check if this is an escaped quote (double quote "")
                // Escaped quotes only occur when we're already inside quotes
                if (inQuotes && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    // This is an escaped quote (""), skip both characters
                    i++;
                    continue;
                }
                // This is a real quote delimiter
                inQuotes = !inQuotes;
                count++;
            }
        }
        
        return count;
    }

    private DrillItemImportRow parseCsvLine(String line, int lineNumber, long currentTime, UUID defaultUserId) {
        String[] values = parseCsvValues(line);
        
        if (values.length < 5) {
            throw new IllegalArgumentException("Line " + lineNumber + " does not have enough columns");
        }

        // CSV columns (in order):
        // drill_group_id, name, description, media_id, media_thumbnail, level_index, 
        // drill_item_order, order_index, passing_score, visibility_code, allow_retry_code, 
        // retry_max, time_limit_ms, level_test, team_id, shots_max, unique_id (optional)
        
        int index = 0;
        // unique_id is optional - may not be present in older CSV files
        String uniqueId = parseString(values, index++);
        UUID drillGroupId = parseUuid(values[index++], "drill_group_id", lineNumber);
        String name = parseString(values, index++);
        String description = parseString(values, index++);
        // Accept string for media_id (URLs, paths, UUIDs, etc.)
        String mediaId = parseString(values, index++);
        String mediaThumbnail = parseString(values, index++);
        Integer levelIndex = parseInt(values, index++, 1);
        Integer drillItemOrder = parseInt(values, index++, 1);
        Integer orderIndex = parseInt(values, index++);
        Integer passingScore = parseInt(values, index++, 3);
        String visibilityCode = parseString(values, index++, "PRIVATE");
        String allowRetryCode = parseString(values, index++, "ACTIVE");
        Integer retryMax = parseInt(values, index++, 3);
        Long timeLimitMs = parseLong(values, index++, 300000L);
        Boolean levelTest = parseBoolean(values, index++, false);
        UUID teamId = parseOptionalUuid(values, index++);
        Integer shotsMax = parseInt(values, index++, 20);

        return DrillItemImportRow.builder()
                .id(UUID.randomUUID())
                .drillGroupId(drillGroupId)
                .name(Optional.ofNullable(name))
                .description(Optional.ofNullable(description))
                .mediaId(Optional.ofNullable(mediaId))
                .mediaThumbnail(Optional.ofNullable(mediaThumbnail))
                .levelIndex(levelIndex)
                .drillItemOrder(drillItemOrder)
                .orderIndex(orderIndex)
                .passingScore(passingScore)
                .visibilityCode(visibilityCode)
                .allowRetryCode(allowRetryCode)
                .retryMax(retryMax)
                .timeLimitMs(timeLimitMs)
                .creationDate(currentTime)
                .modificationDate(currentTime)
                .createdById(defaultUserId)
                .modifiedById(defaultUserId)
                .version(0)
                .levelTest(levelTest)
                .teamId(teamId)
                .shotsMax(shotsMax)
                .uniqueId(Optional.ofNullable(uniqueId))
                .build();
    }

    private String[] parseCsvValues(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                // Check if this is an escaped quote (double quote "")
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // This is an escaped quote, add a single quote to the value
                    currentValue.append('"');
                    i++; // Skip the next quote
                } else {
                    // This is a quote delimiter
                inQuotes = !inQuotes;
                    // Don't add the quote character itself to the value
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                String value = currentValue.toString().trim();
                values.add(value);
                currentValue = new StringBuilder();
            } else {
                // Regular character, add to current value
                currentValue.append(c);
            }
        }
        // Add the last field
        String value = currentValue.toString().trim();
        values.add(value);
        
        return values.toArray(new String[0]);
    }

    private UUID parseUuid(String value, String fieldName, int lineNumber) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Line " + lineNumber + ": " + fieldName + " is required");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Line " + lineNumber + ": Invalid UUID for " + fieldName + ": " + value);
        }
    }

    private UUID parseOptionalUuid(String[] values, int index) {
        if (index >= values.length || values[index] == null || values[index].trim().isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(values[index].trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    private String parseString(String[] values, int index) {
        return parseString(values, index, null);
    }

    private String parseString(String[] values, int index, String defaultValue) {
        if (index >= values.length || values[index] == null || values[index].trim().isEmpty()) {
            return defaultValue;
        }
        String value = values[index].trim();
        return value.equalsIgnoreCase("null") ? defaultValue : value;
    }

    private Integer parseInt(String[] values, int index) {
        return parseInt(values, index, null);
    }

    private Integer parseInt(String[] values, int index, Integer defaultValue) {
        if (index >= values.length || values[index] == null || values[index].trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(values[index].trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long parseLong(String[] values, int index, Long defaultValue) {
        if (index >= values.length || values[index] == null || values[index].trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(values[index].trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Boolean parseBoolean(String[] values, int index, Boolean defaultValue) {
        if (index >= values.length || values[index] == null || values[index].trim().isEmpty()) {
            return defaultValue;
        }
        String value = values[index].trim().toLowerCase();
        return value.equals("true") || value.equals("1") || value.equals("yes");
    }

    /**
     * Get existing media import record by content_url, or create a new one if not found.
     * 
     * @param contentUrl The content URL from CSV
     * @param drillItemName Name from drill item (used for media name)
     * @param drillItemDescription Description from drill item (used for media description)
     * @param currentTime Current timestamp
     * @param defaultUserId Default user ID
     * @return UUID of the media import record
     */
    private UUID getOrCreateMediaImport(String contentUrl, Optional<String> drillItemName, 
                                       Optional<String> drillItemDescription, 
                                       long currentTime, UUID defaultUserId) {
        // Check if media with this content_url already exists
        Optional<MediaImportRow> existingMedia = mediaImportDao.findByContentUrl(contentUrl);
        
        if (existingMedia.isPresent()) {
            logger.debug("Reusing existing media import record for URL: " + contentUrl);
            return existingMedia.get().getId();
        }
        
        // Create new media import record
        String mimeType = detectMimeTypeFromUrl(contentUrl);
        
        MediaImportRow mediaRow = MediaImportRow.builder()
                .id(UUID.randomUUID())
                .name(drillItemName)
                .description(drillItemDescription)
                .contentUrl(Optional.of(contentUrl))
                .statusCode("ACTIVE")
                .mimeType(Optional.ofNullable(mimeType))
                .creationDate(currentTime)
                .modificationDate(currentTime)
                .createdById(defaultUserId)
                .modifiedById(defaultUserId)
                .version(0)
                .build();
        
        UUID mediaId = mediaImportDao.insert(mediaRow);
        logger.debug("Created new media import record for URL: " + contentUrl);
        return mediaId;
    }

    /**
     * Detect MIME type from URL based on file extension.
     * 
     * @param url The content URL
     * @return MIME type string or null if cannot be determined
     */
    private String detectMimeTypeFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Extract file extension
        String lowerUrl = url.toLowerCase();
        int lastDot = lowerUrl.lastIndexOf('.');
        if (lastDot == -1 || lastDot == lowerUrl.length() - 1) {
            return null;
        }
        
        String extension = lowerUrl.substring(lastDot + 1);
        // Remove query parameters if any
        int questionMark = extension.indexOf('?');
        if (questionMark != -1) {
            extension = extension.substring(0, questionMark);
        }
        
        // Common MIME type mappings
        switch (extension) {
            // Video
            case "mp4": return "video/mp4";
            case "webm": return "video/webm";
            case "mov": return "video/quicktime";
            case "avi": return "video/x-msvideo";
            case "mkv": return "video/x-matroska";
            
            // Image
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            case "svg": return "image/svg+xml";
            case "bmp": return "image/bmp";
            
            // Audio
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "ogg": return "audio/ogg";
            case "m4a": return "audio/mp4";
            
            default:
                logger.debug("Unknown file extension: " + extension + " for URL: " + url);
                return null;
        }
    }

    public Optional<DrillItemImportRow> findById(UUID id) {
        return drillItemImportDao.findById(id);
    }

    public List<DrillItemImportRow> findAll() {
        return drillItemImportDao.findAll("");
    }

    @Transactional
    public int deleteAll() {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            return drillItemImportDao.deleteAll();
        });
    }

    /**
     * Download media files from Google Drive and update import tables with local paths.
     * This method:
     * 1. Queries all drill items with group names
     * 2. Queries all media imports with Google Drive URLs
     * 3. Downloads videos and thumbnails to local storage
     * 4. Updates content_url and media_thumbnail with local paths
     * 
     * @param limit Optional limit on the number of items to download. If null, downloads all items.
     * @return DownloadResult containing download statistics
     */
    @Transactional
    public DownloadResult downloadAndUpdateMediaFiles(Integer limit) {
        logger.info("Starting download of media files from Google Drive" + 
                   (limit != null ? " (limit: " + limit + ")" : " (all items)"));
        
        int videosDownloaded = 0;
        int videosFailed = 0;
        int thumbnailsDownloaded = 0;
        int thumbnailsFailed = 0;
        int itemsProcessed = 0;
        long currentTime = System.currentTimeMillis();
        
        try {
            // Get all drill items with group names
            List<com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> drillItems = drillItemImportDao.findAllWithGroupNames();
            logger.info("Found " + drillItems.size() + " drill items to process");
            
            // Create a map of media_id (UUID string) to drill item info for quick lookup
            java.util.Map<String, com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> mediaIdToDrillItem = new java.util.HashMap<>();
            for (com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName item : drillItems) {
                if (item.getMediaId() != null && !item.getMediaId().isEmpty()) {
                    // Try to parse as UUID - if it's a UUID string, use it; otherwise skip
                    try {
                        UUID uuid = UUID.fromString(item.getMediaId());
                        mediaIdToDrillItem.put(uuid.toString(), item);
                    } catch (IllegalArgumentException e) {
                        // media_id is not a UUID (might be a URL), skip for now
                        logger.debug("media_id is not a UUID for drill_item_id: " + item.getId() + ", value: " + item.getMediaId());
                    }
                }
            }
            
            // Download videos from media_import table
            List<MediaImportRow> mediaImports = mediaImportDao.findAllWithGoogleDriveUrls();
            logger.info("Found " + mediaImports.size() + " media imports with Google Drive URLs");
            
            // Rate limiting: pause between downloads to avoid Google Drive throttling
            final long DELAY_BETWEEN_DOWNLOADS_MS = 2000; // 2 seconds between downloads
            final int PROGRESS_LOG_INTERVAL = 10; // Log progress every 10 items
            
            for (MediaImportRow mediaImport : mediaImports) {
                // Check limit for videos
                if (limit != null && itemsProcessed >= limit) {
                    logger.info("Reached limit of " + limit + " items. Stopping video downloads.");
                    break;
                }
                if (mediaImport.getContentUrl().isEmpty()) {
                    continue;
                }
                
                String googleDriveUrl = mediaImport.getContentUrl().get();
                UUID mediaId = mediaImport.getId();
                
                // Find corresponding drill item to get group name, level, and drill name
                com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName drillItem = mediaIdToDrillItem.get(mediaId.toString());
                
                if (drillItem == null) {
                    logger.warn("No drill item found for media_id: " + mediaId + ", skipping download");
                    videosFailed++;
                    itemsProcessed++;
                    continue;
                }
                
                String groupName = drillItem.getDrillGroupName() != null ? drillItem.getDrillGroupName() : "Unknown";
                Integer levelIndex = drillItem.getLevelIndex();
                String drillName = drillItem.getName() != null ? drillItem.getName() : "Unknown";
                
                // Build local file path
                String localVideoPath = googleDriveDownloadService.buildMediaFilePath(
                    groupName, levelIndex, drillName, mediaId.toString(), "video.mp4");
                
                // Log the full path for debugging (especially when drill names are same across groups)
                logger.debug("Downloading video - Group: " + groupName + ", Level: " + levelIndex + 
                           ", Drill: " + drillName + ", MediaId: " + mediaId + 
                           ", Path: " + localVideoPath);
                
                // Log progress periodically
                if (itemsProcessed > 0 && itemsProcessed % PROGRESS_LOG_INTERVAL == 0) {
                    logger.info("Progress: Processed " + itemsProcessed + " items (Videos: " + videosDownloaded + 
                               " downloaded, " + videosFailed + " failed)" + 
                               (limit != null ? " [Limit: " + limit + "]" : ""));
                }
                
                // Download video
                boolean success = googleDriveDownloadService.downloadVideo(googleDriveUrl, localVideoPath);
                
                if (success) {
                    // Update content_url with local path
                    mediaImportDao.updateContentUrl(mediaId, localVideoPath, currentTime);
                    videosDownloaded++;
                    itemsProcessed++;
                    logger.info("Downloaded video " + itemsProcessed + "/" + 
                               (limit != null ? limit : mediaImports.size()) + 
                               " - media_id: " + mediaId);
                } else {
                    videosFailed++;
                    itemsProcessed++;
                    logger.error("Failed to download video for media_id: " + mediaId + " from " + googleDriveUrl);
                }
                
                // Pause between downloads to avoid rate limiting (except when limit is reached)
                if (limit == null || itemsProcessed < limit) {
                    try {
                        Thread.sleep(DELAY_BETWEEN_DOWNLOADS_MS);
                    } catch (InterruptedException e) {
                        logger.warn("Download pause interrupted: " + e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // Download thumbnails from drill_item_import table
            int thumbnailCount = 0;
            for (com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName drillItem : drillItems) {
                // Check limit for thumbnails (continue counting from videos)
                if (limit != null && itemsProcessed >= limit) {
                    logger.info("Reached limit of " + limit + " items. Stopping thumbnail downloads.");
                    break;
                }
                if (drillItem.getMediaThumbnail() == null || drillItem.getMediaThumbnail().isEmpty()) {
                    continue;
                }
                
                String thumbnailUrl = drillItem.getMediaThumbnail();
                
                // Check if it's a Google Drive URL
                if (!thumbnailUrl.contains("drive.google.com")) {
                    continue; // Skip non-Google Drive URLs
                }
                
                String groupName = drillItem.getDrillGroupName() != null ? drillItem.getDrillGroupName() : "Unknown";
                Integer levelIndex = drillItem.getLevelIndex();
                String drillName = drillItem.getName() != null ? drillItem.getName() : "Unknown";
                
                // Use media_id if available, otherwise use drill_item_id
                String mediaIdStr = drillItem.getMediaId() != null && !drillItem.getMediaId().isEmpty() 
                    ? drillItem.getMediaId() 
                    : drillItem.getId().toString();
                
                // Build local file path
                String localThumbnailPath = googleDriveDownloadService.buildMediaFilePath(
                    groupName, levelIndex, drillName, mediaIdStr, "thumbnail.jpg");
                
                // Log the full path for debugging (especially when drill names are same across groups)
                logger.debug("Downloading thumbnail - Group: " + groupName + ", Level: " + levelIndex + 
                           ", Drill: " + drillName + ", MediaId: " + mediaIdStr + 
                           ", Path: " + localThumbnailPath);
                
                // Log progress periodically for thumbnails
                thumbnailCount++;
                if (thumbnailCount > 0 && thumbnailCount % PROGRESS_LOG_INTERVAL == 0) {
                    logger.info("Progress: Processed " + itemsProcessed + " total items (Thumbnails: " + 
                               thumbnailsDownloaded + " downloaded, " + thumbnailsFailed + " failed)" +
                               (limit != null ? " [Limit: " + limit + "]" : ""));
                }
                
                // Download thumbnail
                boolean success = googleDriveDownloadService.downloadThumbnail(thumbnailUrl, localThumbnailPath);
                
                if (success) {
                    // Update media_thumbnail with local path
                    drillItemImportDao.updateMediaThumbnail(drillItem.getId(), localThumbnailPath, currentTime);
                    thumbnailsDownloaded++;
                    itemsProcessed++;
                    logger.info("Downloaded thumbnail " + itemsProcessed + "/" + 
                               (limit != null ? limit : "all") + 
                               " - drill_item_id: " + drillItem.getId());
                } else {
                    thumbnailsFailed++;
                    itemsProcessed++;
                    logger.error("Failed to download thumbnail for drill_item_id: " + drillItem.getId() + " from " + thumbnailUrl);
                }
                
                // Pause between downloads to avoid rate limiting
                if (limit == null || itemsProcessed < limit) {
                    try {
                        Thread.sleep(DELAY_BETWEEN_DOWNLOADS_MS);
                    } catch (InterruptedException e) {
                        logger.warn("Download pause interrupted: " + e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            logger.info("Download completed - Videos: " + videosDownloaded + " downloaded, " + videosFailed + " failed. " +
                       "Thumbnails: " + thumbnailsDownloaded + " downloaded, " + thumbnailsFailed + " failed");
            
            return new DownloadResult(videosDownloaded, videosFailed, thumbnailsDownloaded, thumbnailsFailed);
            
        } catch (Exception e) {
            logger.error("Error during media file download", e);
            throw new TransactionException(e);
        }
    }

    /**
     * Download media files from Google Drive in production tables and replace URLs with local paths.
     * This method works on production tables (t_drill_item, t_media) instead of import tables.
     * 
     * This method:
     * 1. Queries all media records in t_media with Google Drive URLs
     * 2. Queries all drill items in t_drill_item with Google Drive thumbnails
     * 3. Downloads videos and thumbnails to local storage
     * 4. Updates content_url in t_media and media_thumbnail in t_drill_item with local paths
     * 
     * Note: Not using @Transactional because downloads can take a long time and cause transaction timeouts.
     * Each database update is done in its own transaction.
     * 
     * @param limit Optional limit on the number of items to download. If null, downloads all items.
     * @return DownloadResult containing download statistics
     */
    public DownloadResult downloadAndUpdateProductionMediaFiles(Integer limit) {
        logger.info("Starting download of media files from Google Drive (production tables)" + 
                   (limit != null ? " (limit: " + limit + ")" : " (all items)"));
        
        int videosDownloaded = 0;
        int videosFailed = 0;
        int thumbnailsDownloaded = 0;
        int thumbnailsFailed = 0;
        int itemsProcessed = 0;
        long currentTime = System.currentTimeMillis();
        
        // Rate limiting: pause between downloads to avoid Google Drive throttling
        final long DELAY_BETWEEN_DOWNLOADS_MS = 2000; // 2 seconds between downloads
        final int PROGRESS_LOG_INTERVAL = 10; // Log progress every 10 items
        
        try {
            // Get all drill items with group names from production table
            List<com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> drillItems = drillItemDao.findAllWithGroupNames();
            logger.info("Found " + drillItems.size() + " drill items in production table");
            
            // Create a map of media_id (UUID string) to drill item info for quick lookup
            java.util.Map<String, com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> mediaIdToDrillItem = new java.util.HashMap<>();
            for (com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName item : drillItems) {
                if (item.getMediaId() != null && !item.getMediaId().isEmpty()) {
                    // Try to parse as UUID - if it's a UUID string, use it; otherwise skip
                    try {
                        UUID uuid = UUID.fromString(item.getMediaId());
                        mediaIdToDrillItem.put(uuid.toString(), item);
                    } catch (IllegalArgumentException e) {
                        // media_id is not a UUID (might be a URL), skip for now
                        logger.debug("media_id is not a UUID for drill_item_id: " + item.getId() + ", value: " + item.getMediaId());
                    }
                }
            }
            
            // Download videos from production t_media table
            List<MediaDao.MediaContentUrlRow> mediaRecords = mediaDao.findAllWithGoogleDriveContentUrls();
            logger.info("Found " + mediaRecords.size() + " media records with Google Drive URLs in production table");
            
            // Debug: Log sample URLs if any found
            if (mediaRecords.size() > 0) {
                logger.info("Sample media URL: " + mediaRecords.get(0).getContentUrl());
            } else {
                // Check total count of media records with any content_url
                long totalMediaCount = jdbiProvider.getJdbi().withHandle(handle -> 
                    handle.createQuery("SELECT COUNT(*) FROM t_media WHERE content_url IS NOT NULL")
                        .mapTo(Long.class)
                        .one()
                );
                logger.info("Total media records with non-null content_url: " + totalMediaCount);
                
                // Check sample URLs
                List<String> sampleUrls = jdbiProvider.getJdbi().withHandle(handle ->
                    handle.createQuery("SELECT content_url FROM t_media WHERE content_url IS NOT NULL LIMIT 5")
                        .mapTo(String.class)
                        .list()
                );
                if (!sampleUrls.isEmpty()) {
                    logger.info("Sample content_url values: " + sampleUrls);
                }
            }
            
            for (MediaDao.MediaContentUrlRow mediaRecord : mediaRecords) {
                // Check limit for videos
                if (limit != null && itemsProcessed >= limit) {
                    logger.info("Reached limit of " + limit + " items. Stopping video downloads.");
                    break;
                }
                
                UUID mediaId = mediaRecord.getId();
                String googleDriveUrl = mediaRecord.getContentUrl();
                
                // Find corresponding drill item(s) to get group name, level, and drill name
                List<com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> associatedDrillItems = drillItemDao.findByMediaId(mediaId);
                
                if (associatedDrillItems == null || associatedDrillItems.isEmpty()) {
                    logger.warn("No drill item found for media_id: " + mediaId + ", skipping download");
                    videosFailed++;
                    itemsProcessed++;
                    continue;
                }
                
                // Use the first drill item for path building (if multiple exist, use first one)
                com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName drillItem = associatedDrillItems.get(0);
                
                String groupName = drillItem.getDrillGroupName() != null ? drillItem.getDrillGroupName() : "Unknown";
                Integer levelIndex = drillItem.getLevelIndex();
                String drillName = drillItem.getName() != null ? drillItem.getName() : "Unknown";
                
                // Build local file path
                String localVideoPath = googleDriveDownloadService.buildMediaFilePath(
                    groupName, levelIndex, drillName, mediaId.toString(), "video.mp4");
                
                // Log progress periodically
                if (itemsProcessed > 0 && itemsProcessed % PROGRESS_LOG_INTERVAL == 0) {
                    logger.info("Progress: Processed " + itemsProcessed + " items (Videos: " + videosDownloaded + 
                               " downloaded, " + videosFailed + " failed)" + 
                               (limit != null ? " [Limit: " + limit + "]" : ""));
                }
                
                // Download video
                boolean success = googleDriveDownloadService.downloadVideo(googleDriveUrl, localVideoPath);
                
                if (success) {
                    // Update content_url in production t_media table with local path
                    mediaDao.updateContentUrl(mediaId, localVideoPath, currentTime);
                    videosDownloaded++;
                    itemsProcessed++;
                    logger.info("Downloaded video " + itemsProcessed + "/" + 
                               (limit != null ? limit : mediaRecords.size()) + 
                               " - media_id: " + mediaId);
                } else {
                    videosFailed++;
                    itemsProcessed++;
                    logger.error("Failed to download video for media_id: " + mediaId + " from " + googleDriveUrl);
                }
                
                // Pause between downloads to avoid rate limiting
                if (limit == null || itemsProcessed < limit) {
                    try {
                        Thread.sleep(DELAY_BETWEEN_DOWNLOADS_MS);
                    } catch (InterruptedException e) {
                        logger.warn("Download pause interrupted: " + e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // Download thumbnails from production t_drill_item table
            List<DrillItemDao.DrillItemThumbnailRow> thumbnailRecords = drillItemDao.findAllWithGoogleDriveThumbnails();
            logger.info("Found " + thumbnailRecords.size() + " drill items with Google Drive thumbnails in production table");
            
            // Debug: Log sample URLs if any found
            if (thumbnailRecords.size() > 0) {
                logger.info("Sample thumbnail URL: " + thumbnailRecords.get(0).getMediaThumbnail());
            } else {
                // Check total count of drill items with any media_thumbnail
                long totalThumbnailCount = jdbiProvider.getJdbi().withHandle(handle ->
                    handle.createQuery("SELECT COUNT(*) FROM t_drill_item WHERE media_thumbnail IS NOT NULL")
                        .mapTo(Long.class)
                        .one()
                );
                logger.info("Total drill items with non-null media_thumbnail: " + totalThumbnailCount);
                
                // Check sample URLs
                List<String> sampleThumbnails = jdbiProvider.getJdbi().withHandle(handle ->
                    handle.createQuery("SELECT media_thumbnail FROM t_drill_item WHERE media_thumbnail IS NOT NULL LIMIT 5")
                        .mapTo(String.class)
                        .list()
                );
                if (!sampleThumbnails.isEmpty()) {
                    logger.info("Sample media_thumbnail values: " + sampleThumbnails);
                }
            }
            
            int thumbnailCount = 0;
            for (DrillItemDao.DrillItemThumbnailRow thumbnailRecord : thumbnailRecords) {
                // Check limit for thumbnails (continue counting from videos)
                if (limit != null && itemsProcessed >= limit) {
                    logger.info("Reached limit of " + limit + " items. Stopping thumbnail downloads.");
                    break;
                }
                
                UUID drillItemId = thumbnailRecord.getId();
                String thumbnailUrl = thumbnailRecord.getMediaThumbnail();
                
                // Find the drill item details
                com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName drillItem = null;
                for (com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName item : drillItems) {
                    if (item.getId().equals(drillItemId)) {
                        drillItem = item;
                        break;
                    }
                }
                
                if (drillItem == null) {
                    logger.warn("Drill item not found for drill_item_id: " + drillItemId + ", skipping thumbnail download");
                    thumbnailsFailed++;
                    itemsProcessed++;
                    continue;
                }
                
                String groupName = drillItem.getDrillGroupName() != null ? drillItem.getDrillGroupName() : "Unknown";
                Integer levelIndex = drillItem.getLevelIndex();
                String drillName = drillItem.getName() != null ? drillItem.getName() : "Unknown";
                
                // Use media_id if available, otherwise use drill_item_id
                String mediaIdStr = drillItem.getMediaId() != null && !drillItem.getMediaId().isEmpty() 
                    ? drillItem.getMediaId() 
                    : drillItem.getId().toString();
                
                // Build local file path
                String localThumbnailPath = googleDriveDownloadService.buildMediaFilePath(
                    groupName, levelIndex, drillName, mediaIdStr, "thumbnail.jpg");
                
                // Log progress periodically for thumbnails
                thumbnailCount++;
                if (thumbnailCount > 0 && thumbnailCount % PROGRESS_LOG_INTERVAL == 0) {
                    logger.info("Progress: Processed " + itemsProcessed + " total items (Thumbnails: " + 
                               thumbnailsDownloaded + " downloaded, " + thumbnailsFailed + " failed)" +
                               (limit != null ? " [Limit: " + limit + "]" : ""));
                }
                
                // Download thumbnail
                boolean success = googleDriveDownloadService.downloadThumbnail(thumbnailUrl, localThumbnailPath);
                
                if (success) {
                    // Update media_thumbnail in production t_drill_item table with local path
                    drillItemDao.updateMediaThumbnail(drillItemId, localThumbnailPath, currentTime);
                    thumbnailsDownloaded++;
                    itemsProcessed++;
                    logger.info("Downloaded thumbnail " + itemsProcessed + "/" + 
                               (limit != null ? limit : "all") + 
                               " - drill_item_id: " + drillItemId);
                } else {
                    thumbnailsFailed++;
                    itemsProcessed++;
                    logger.error("Failed to download thumbnail for drill_item_id: " + drillItemId + " from " + thumbnailUrl);
                }
                
                // Pause between downloads to avoid rate limiting
                if (limit == null || itemsProcessed < limit) {
                    try {
                        Thread.sleep(DELAY_BETWEEN_DOWNLOADS_MS);
                    } catch (InterruptedException e) {
                        logger.warn("Download pause interrupted: " + e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            logger.info("Download completed - Videos: " + videosDownloaded + " downloaded, " + videosFailed + " failed. " +
                       "Thumbnails: " + thumbnailsDownloaded + " downloaded, " + thumbnailsFailed + " failed");
            
            return new DownloadResult(videosDownloaded, videosFailed, thumbnailsDownloaded, thumbnailsFailed);
            
        } catch (Exception e) {
            logger.error("Error during production media file download", e);
            throw new TransactionException(e);
        }
    }

    /**
     * Migrate all data from import tables to production tables.
     * This method:
     * 1. Downloads media files from Google Drive and updates import tables with local paths
     * 2. Deletes all data from t_drill_item and t_media
     * 3. Copies all data from t_media_import to t_media
     * 4. Copies all data from t_drill_item_import to t_drill_item (with UUID conversion for media_id)
     * 
     * @param limit Optional limit on the number of items to download. If null, downloads all items.
     * @return MigrationResult containing counts of migrated records and download statistics
     */
    public MigrationResult migrateImportToProduction(Integer limit) {
        // Step 0: Download media files from Google Drive and update import tables with local paths
        DownloadResult downloadResult = downloadAndUpdateMediaFiles(limit);
        
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.READ_COMMITTED, handle -> {
            try {
                logger.info("Starting migration from import tables to production tables");
                
                // Get DAOs attached to this transaction handle
                DrillItemImportDao drillItemDao = handle.attach(DrillItemImportDao.class);
                MediaImportDao mediaDao = handle.attach(MediaImportDao.class);
                
                // Get counts before deleting
                int deletedDrillItems = handle.createQuery("SELECT COUNT(*) FROM t_drill_item")
                        .mapTo(Integer.class)
                        .one();
                int deletedMedia = handle.createQuery("SELECT COUNT(*) FROM t_media")
                        .mapTo(Integer.class)
                        .one();
                
                // Step 1: Check if we need to delete from t_drill
                // Only necessary if imported IDs differ from existing IDs (would cause FK violations)
                // If IDs match, we can preserve athlete drill submission history
                try {
                    // Check if there are any t_drill records that reference drill_items that won't exist after migration
                    Long drillsWithNonMatchingIds = handle.createQuery(
                        "SELECT COUNT(*) FROM t_drill d " +
                        "WHERE d.drill_item_id NOT IN (SELECT id FROM t_drill_item_import)")
                        .mapTo(Long.class)
                        .one();
                    
                    if (drillsWithNonMatchingIds > 0) {
                        // IDs don't match - need to delete t_drill to avoid FK violations
                        logger.warn("Found " + drillsWithNonMatchingIds + " t_drill records with non-matching drill_item_ids. " +
                                   "Deleting t_drill records to prevent FK constraint violations. " +
                                   "Note: This will delete athlete drill submission history.");
                        int deletedDrills = handle.createUpdate("DELETE FROM t_drill").execute();
                        logger.info("Deleted " + deletedDrills + " records from t_drill");
                    } else {
                        logger.info("All t_drill records reference drill_items that exist in import table. " +
                                   "Preserving athlete drill submission history.");
                    }
                } catch (Exception e) {
                    // If check fails, delete t_drill to be safe (prevents FK violations)
                    logger.warn("Could not verify t_drill references. Deleting t_drill to prevent FK violations: " + e.getMessage());
                    try {
                        int deletedDrills = handle.createUpdate("DELETE FROM t_drill").execute();
                        if (deletedDrills > 0) {
                            logger.info("Deleted " + deletedDrills + " records from t_drill");
                        }
                    } catch (Exception deleteEx) {
                        logger.debug("Could not delete from t_drill: " + deleteEx.getMessage());
                    }
                }
                
                // Step 2: Delete all from t_drill_item (references t_media)
                drillItemDao.deleteAllFromProduction();
                logger.info("Deleted " + deletedDrillItems + " records from t_drill_item");
                
                // Step 3: Delete all from t_media
                mediaDao.deleteAllFromProduction();
                logger.info("Deleted " + deletedMedia + " records from t_media");
                
                // Step 3.5: Update old media store paths in import tables to use current configured path
                String oldPathPrefix = "/Volumes/mnt/thrones/pallbearer/media";
                String newPathPrefix = mediaStorePath;
                long currentTime = System.currentTimeMillis();
                
                if (!oldPathPrefix.equals(newPathPrefix)) {
                    logger.info("Updating media store paths in import tables: " + oldPathPrefix + " -> " + newPathPrefix);
                    
                    // Update content_url paths in t_media_import
                    int updatedMediaPaths = mediaDao.updateContentUrlPathPrefix(oldPathPrefix, newPathPrefix, currentTime);
                    logger.info("Updated " + updatedMediaPaths + " content_url paths in t_media_import");
                    
                    // Update media_thumbnail paths in t_drill_item_import
                    int updatedThumbnailPaths = drillItemDao.updateMediaThumbnailPathPrefix(oldPathPrefix, newPathPrefix, currentTime);
                    logger.info("Updated " + updatedThumbnailPaths + " media_thumbnail paths in t_drill_item_import");
                } else {
                    logger.info("Media store path unchanged, skipping path update");
                }
                
                // Step 4: Copy from t_media_import to t_media
                int migratedMedia = mediaDao.migrateToProduction();
                logger.info("Migrated " + migratedMedia + " records from t_media_import to t_media");
                
                // Step 5: Copy from t_drill_item_import to t_drill_item
                int migratedDrillItems = drillItemDao.migrateToProduction();
                logger.info("Migrated " + migratedDrillItems + " records from t_drill_item_import to t_drill_item");
                
                logger.info("Migration completed successfully");
                
                return new MigrationResult(deletedDrillItems, deletedMedia, migratedMedia, migratedDrillItems, downloadResult);
            } catch (Exception e) {
                logger.error("Error during migration from import tables to production", e);
                throw new TransactionException(e);
            }
        });
    }

    /**
     * Result object for download operation
     */
    public static class DownloadResult {
        private final int videosDownloaded;
        private final int videosFailed;
        private final int thumbnailsDownloaded;
        private final int thumbnailsFailed;

        public DownloadResult(int videosDownloaded, int videosFailed, int thumbnailsDownloaded, int thumbnailsFailed) {
            this.videosDownloaded = videosDownloaded;
            this.videosFailed = videosFailed;
            this.thumbnailsDownloaded = thumbnailsDownloaded;
            this.thumbnailsFailed = thumbnailsFailed;
        }

        public int getVideosDownloaded() {
            return videosDownloaded;
        }

        public int getVideosFailed() {
            return videosFailed;
        }

        public int getThumbnailsDownloaded() {
            return thumbnailsDownloaded;
        }

        public int getThumbnailsFailed() {
            return thumbnailsFailed;
        }
    }

    /**
     * Result object for migration operation
     */
    public static class MigrationResult {
        private final int deletedDrillItems;
        private final int deletedMedia;
        private final int migratedMedia;
        private final int migratedDrillItems;
        private final DownloadResult downloadResult;

        public MigrationResult(int deletedDrillItems, int deletedMedia, int migratedMedia, int migratedDrillItems) {
            this.deletedDrillItems = deletedDrillItems;
            this.deletedMedia = deletedMedia;
            this.migratedMedia = migratedMedia;
            this.migratedDrillItems = migratedDrillItems;
            this.downloadResult = null;
        }

        public MigrationResult(int deletedDrillItems, int deletedMedia, int migratedMedia, int migratedDrillItems, DownloadResult downloadResult) {
            this.deletedDrillItems = deletedDrillItems;
            this.deletedMedia = deletedMedia;
            this.migratedMedia = migratedMedia;
            this.migratedDrillItems = migratedDrillItems;
            this.downloadResult = downloadResult;
        }

        public int getDeletedDrillItems() {
            return deletedDrillItems;
        }

        public int getDeletedMedia() {
            return deletedMedia;
        }

        public int getMigratedMedia() {
            return migratedMedia;
        }

        public int getMigratedDrillItems() {
            return migratedDrillItems;
        }

        public DownloadResult getDownloadResult() {
            return downloadResult;
        }
    }

    /**
     * Result object for sync operation
     */
    public static class SyncResult {
        private final int videosDownloaded;
        private final int videosFailed;
        private final int thumbnailsDownloaded;
        private final int thumbnailsFailed;
        private final int drillsProcessed;
        private final int drillsSkipped;

        public SyncResult(int videosDownloaded, int videosFailed, int thumbnailsDownloaded, int thumbnailsFailed, 
                         int drillsProcessed, int drillsSkipped) {
            this.videosDownloaded = videosDownloaded;
            this.videosFailed = videosFailed;
            this.thumbnailsDownloaded = thumbnailsDownloaded;
            this.thumbnailsFailed = thumbnailsFailed;
            this.drillsProcessed = drillsProcessed;
            this.drillsSkipped = drillsSkipped;
        }

        public int getVideosDownloaded() {
            return videosDownloaded;
        }

        public int getVideosFailed() {
            return videosFailed;
        }

        public int getThumbnailsDownloaded() {
            return thumbnailsDownloaded;
        }

        public int getThumbnailsFailed() {
            return thumbnailsFailed;
        }

        public int getDrillsProcessed() {
            return drillsProcessed;
        }

        public int getDrillsSkipped() {
            return drillsSkipped;
        }
    }

    /**
     * Sync missing videos and thumbnails from import tables to production tables.
     * This method:
     * 1. Iterates through all production drill items one by one
     * 2. For each production drill item, finds the corresponding import drill item by unique_id
     * 3. Checks if import table has Google Drive URLs for video/thumbnail
     * 4. Checks if production table is missing local files (has Google Drive URL or null/empty)
     * 5. Downloads videos and thumbnails from import table's Google Drive URLs
     * 6. Updates production tables with local paths
     * 7. Does NOT modify import tables (they are for backup/reference only)
     * 
     * @param limit Optional limit on the number of drill items to process. If null, processes all items.
     * @return SyncResult containing download statistics
     */
    public SyncResult syncMissingMediaFromImport(Integer limit) {
        logger.info("Starting sync of missing media from import tables to production tables" + 
                   (limit != null ? " (limit: " + limit + ")" : " (all items)"));
        
        int videosDownloaded = 0;
        int videosFailed = 0;
        int thumbnailsDownloaded = 0;
        int thumbnailsFailed = 0;
        int drillsProcessed = 0;
        int drillsSkipped = 0;
        long currentTime = System.currentTimeMillis();
        
        // Rate limiting: pause between downloads to avoid Google Drive throttling
        final long DELAY_BETWEEN_DOWNLOADS_MS = 2000; // 2 seconds between downloads
        final int PROGRESS_LOG_INTERVAL = 10; // Log progress every 10 items
        
        try {
            // Get all production drill items with group names
            List<com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> productionDrillItems = drillItemDao.findAllWithGroupNames();
            logger.info("Found " + productionDrillItems.size() + " production drill items to process");
            
            for (com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName productionDrillItem : productionDrillItems) {
                // Check limit
                if (limit != null && drillsProcessed >= limit) {
                    logger.info("Reached limit of " + limit + " drill items. Stopping sync.");
                    break;
                }
                
                // Skip if no unique_id (can't match with import table)
                if (productionDrillItem.getUniqueId() == null || productionDrillItem.getUniqueId().trim().isEmpty()) {
                    logger.debug("Skipping drill item " + productionDrillItem.getId() + " - no unique_id");
                    drillsSkipped++;
                    continue;
                }
                
                // Find corresponding import drill item by unique_id
                Optional<DrillItemImportRow> importDrillItemOpt = drillItemImportDao.findByUniqueId(productionDrillItem.getUniqueId().trim());
                
                if (!importDrillItemOpt.isPresent()) {
                    logger.debug("No import drill item found for unique_id: " + productionDrillItem.getUniqueId() + " (drill_item_id: " + productionDrillItem.getId() + ")");
                    drillsSkipped++;
                    continue;
                }
                
                DrillItemImportRow importDrillItem = importDrillItemOpt.get();
                drillsProcessed++;
                
                String groupName = productionDrillItem.getDrillGroupName() != null ? productionDrillItem.getDrillGroupName() : "Unknown";
                Integer levelIndex = productionDrillItem.getLevelIndex();
                String drillName = productionDrillItem.getName() != null ? productionDrillItem.getName() : "Unknown";
                
                // Process video (from media table)
                if (productionDrillItem.getMediaId() != null && !productionDrillItem.getMediaId().isEmpty()) {
                    try {
                        UUID mediaId = UUID.fromString(productionDrillItem.getMediaId());
                        
                        // Get production media content_url directly using JDBI handle
                        String productionContentUrl = jdbiProvider.getJdbi().withHandle(handle ->
                            handle.createQuery("SELECT content_url FROM t_media WHERE id = :mediaId")
                                .bind("mediaId", mediaId)
                                .mapTo(String.class)
                                .findOne()
                                .orElse(null)
                        );
                        
                        // Check if production has Google Drive URL or is missing (needs download)
                        boolean needsVideoDownload = (productionContentUrl == null || productionContentUrl.isEmpty() || 
                                                     productionContentUrl.contains("drive.google.com"));
                        
                        if (needsVideoDownload) {
                            // Get import media record
                            String importMediaIdStr = importDrillItem.getMediaId().orElse(null);
                            if (importMediaIdStr != null && !importMediaIdStr.isEmpty()) {
                                try {
                                    UUID importMediaId = UUID.fromString(importMediaIdStr);
                                    Optional<MediaImportRow> importMediaOpt = mediaImportDao.findById(importMediaId);
                                    
                                    if (importMediaOpt.isPresent()) {
                                        MediaImportRow importMedia = importMediaOpt.get();
                                        String importContentUrl = importMedia.getContentUrl().orElse(null);
                                        
                                        // Check if import has Google Drive URL
                                        if (importContentUrl != null && !importContentUrl.isEmpty() && 
                                            importContentUrl.contains("drive.google.com")) {
                                            
                                            // Build local file path
                                            String localVideoPath = googleDriveDownloadService.buildMediaFilePath(
                                                groupName, levelIndex, drillName, mediaId.toString(), "video.mp4");
                                            
                                            // Download video
                                            boolean success = googleDriveDownloadService.downloadVideo(importContentUrl, localVideoPath);
                                            
                                            if (success) {
                                                // Update production media table with local path
                                                mediaDao.updateContentUrl(mediaId, localVideoPath, currentTime);
                                                videosDownloaded++;
                                                logger.info("Downloaded and updated video for drill_item_id: " + productionDrillItem.getId() + 
                                                           ", media_id: " + mediaId);
                                            } else {
                                                videosFailed++;
                                                logger.error("Failed to download video for drill_item_id: " + productionDrillItem.getId() + 
                                                           ", media_id: " + mediaId + " from " + importContentUrl);
                                            }
                                            
                                            // Pause between downloads
                                            try {
                                                Thread.sleep(DELAY_BETWEEN_DOWNLOADS_MS);
                                            } catch (InterruptedException e) {
                                                logger.warn("Download pause interrupted: " + e.getMessage());
                                                Thread.currentThread().interrupt();
                                                break;
                                            }
                                        }
                                    }
                                } catch (IllegalArgumentException e) {
                                    logger.debug("Import media_id is not a valid UUID: " + importMediaIdStr);
                                }
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        logger.debug("Production media_id is not a valid UUID: " + productionDrillItem.getMediaId());
                    }
                }
                
                // Process thumbnail (from drill_item table)
                String productionThumbnail = productionDrillItem.getMediaThumbnail();
                boolean needsThumbnailDownload = (productionThumbnail == null || productionThumbnail.isEmpty() || 
                                                  productionThumbnail.contains("drive.google.com"));
                
                if (needsThumbnailDownload) {
                    String importThumbnail = importDrillItem.getMediaThumbnail().orElse(null);
                    
                    // Check if import has Google Drive URL for thumbnail
                    if (importThumbnail != null && !importThumbnail.isEmpty() && 
                        importThumbnail.contains("drive.google.com")) {
                        
                        // Use media_id if available, otherwise use drill_item_id
                        String mediaIdStr = productionDrillItem.getMediaId() != null && !productionDrillItem.getMediaId().isEmpty() 
                            ? productionDrillItem.getMediaId() 
                            : productionDrillItem.getId().toString();
                        
                        // Build local file path
                        String localThumbnailPath = googleDriveDownloadService.buildMediaFilePath(
                            groupName, levelIndex, drillName, mediaIdStr, "thumbnail.jpg");
                        
                        // Download thumbnail
                        boolean success = googleDriveDownloadService.downloadThumbnail(importThumbnail, localThumbnailPath);
                        
                        if (success) {
                            // Update production drill_item table with local path
                            drillItemDao.updateMediaThumbnail(productionDrillItem.getId(), localThumbnailPath, currentTime);
                            thumbnailsDownloaded++;
                            logger.info("Downloaded and updated thumbnail for drill_item_id: " + productionDrillItem.getId());
                        } else {
                            thumbnailsFailed++;
                            logger.error("Failed to download thumbnail for drill_item_id: " + productionDrillItem.getId() + 
                                       " from " + importThumbnail);
                        }
                        
                        // Pause between downloads
                        try {
                            Thread.sleep(DELAY_BETWEEN_DOWNLOADS_MS);
                        } catch (InterruptedException e) {
                            logger.warn("Download pause interrupted: " + e.getMessage());
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                // Log progress periodically
                if (drillsProcessed > 0 && drillsProcessed % PROGRESS_LOG_INTERVAL == 0) {
                    logger.info("Progress: Processed " + drillsProcessed + " drill items (Videos: " + videosDownloaded + 
                               " downloaded, " + videosFailed + " failed. Thumbnails: " + thumbnailsDownloaded + 
                               " downloaded, " + thumbnailsFailed + " failed)" + 
                               (limit != null ? " [Limit: " + limit + "]" : ""));
                }
            }
            
            logger.info("Sync completed - Processed: " + drillsProcessed + " drill items, Skipped: " + drillsSkipped + 
                       ". Videos: " + videosDownloaded + " downloaded, " + videosFailed + " failed. " +
                       "Thumbnails: " + thumbnailsDownloaded + " downloaded, " + thumbnailsFailed + " failed");
            
            return new SyncResult(videosDownloaded, videosFailed, thumbnailsDownloaded, thumbnailsFailed, 
                                 drillsProcessed, drillsSkipped);
            
        } catch (Exception e) {
            logger.error("Error during sync of missing media from import tables", e);
            throw new TransactionException(e);
        }
    }

    /**
     * Downloads media files from Google Drive URLs in CSV and organizes them by group name and unique_id.
     * This method:
     * 1. Parses CSV file (same format as drill_item_import)
     * 2. Extracts media_id (video) and media_thumbnail (thumbnail) Google Drive URLs
     * 3. Downloads videos and thumbnails to folders organized by group name and unique_id
     * 4. Updates database records with local file paths
     * 
     * Folder structure: {mediaStorePath}/{groupName}/{unique_id}/video.mp4 and thumbnail.jpg
     * 
     * @param csvInputStream CSV input stream
     * @return DownloadResult containing download statistics
     */
    public DownloadResult downloadMediaFromCsv(InputStream csvInputStream) {
        logger.info("Starting download of media files from CSV");
        
        int videosDownloaded = 0;
        int videosFailed = 0;
        int thumbnailsDownloaded = 0;
        int thumbnailsFailed = 0;
        int rowsProcessed = 0;
        long currentTime = System.currentTimeMillis();
        
        // Rate limiting: pause between downloads to avoid Google Drive throttling
        final long DELAY_BETWEEN_DOWNLOADS_MS = 2000; // 2 seconds between downloads
        final int PROGRESS_LOG_INTERVAL = 10; // Log progress every 10 items
        
        try {
            // Parse CSV
            CurrentUser auditUser = userService.getCurrentUser();
            List<DrillItemImportRow> rows;
            try {
                rows = parseCsv(csvInputStream, auditUser);
            } catch (Exception e) {
                logger.error("Error parsing CSV file", e);
                throw new TransactionException(e);
            }
            
            if (rows.isEmpty()) {
                logger.warn("No rows found in CSV file");
                return new DownloadResult(0, 0, 0, 0);
            }
            
            logger.info("Found " + rows.size() + " rows in CSV to process");
            
            // Process each row
            for (DrillItemImportRow row : rows) {
                rowsProcessed++;
                
                try {
                    // Get group name from drill_group_id
                    String groupName = getGroupNameFromId(row.getDrillGroupId());
                    if (groupName == null || groupName.isEmpty()) {
                        groupName = "Unknown";
                    }
                    
                    // Get unique_id - use it for folder name
                    String uniqueId = row.getUniqueId().orElse(null);
                    if (uniqueId == null || uniqueId.trim().isEmpty()) {
                        logger.warn("Row " + rowsProcessed + " has no unique_id, skipping");
                        continue;
                    }
                    uniqueId = uniqueId.trim();
                    
                    // Sanitize group name and unique_id for file system
                    String sanitizedGroup = googleDriveDownloadService.sanitizeFileName(groupName);
                    String sanitizedUniqueId = googleDriveDownloadService.sanitizeFileName(uniqueId);
                    
                    // Process video (media_id)
                    if (row.getMediaId().isPresent() && !row.getMediaId().get().isEmpty()) {
                        String mediaIdUrl = row.getMediaId().get();
                        
                        // Check if it's a Google Drive URL
                        if (mediaIdUrl.contains("drive.google.com")) {
                            // Build local file path: {groupName}/{unique_id}/video.mp4
                            String folderPath = String.format("%s/%s/%s",
                                    mediaStorePath,
                                    sanitizedGroup,
                                    sanitizedUniqueId);
                            String localVideoPath = folderPath + "/video.mp4";
                            
                            logger.debug("Downloading video - Group: " + groupName + ", UniqueId: " + uniqueId + 
                                       ", Path: " + localVideoPath);
                            
                            // Download video
                            boolean success = googleDriveDownloadService.downloadVideo(mediaIdUrl, localVideoPath);
                            
                            if (success) {
                                videosDownloaded++;
                                logger.info("Downloaded video " + videosDownloaded + "/" + rows.size() + 
                                           " - Group: " + groupName + ", UniqueId: " + uniqueId);
                                
                                // Update media import record if it exists
                                // First, try to find existing media import by content_url
                                Optional<MediaImportRow> existingMedia = mediaImportDao.findByContentUrl(mediaIdUrl);
                                if (existingMedia.isPresent()) {
                                    mediaImportDao.updateContentUrl(existingMedia.get().getId(), localVideoPath, currentTime);
                                }
                            } else {
                                videosFailed++;
                                logger.error("Failed to download video for Group: " + groupName + 
                                           ", UniqueId: " + uniqueId + " from " + mediaIdUrl);
                            }
                            
                            // Pause between downloads
                            try {
                                Thread.sleep(DELAY_BETWEEN_DOWNLOADS_MS);
                            } catch (InterruptedException e) {
                                logger.warn("Download pause interrupted: " + e.getMessage());
                                Thread.currentThread().interrupt();
                                break;
                            }
                        } else {
                            logger.debug("Skipping non-Google Drive URL for media_id: " + mediaIdUrl);
                        }
                    }
                    
                    // Process thumbnail (media_thumbnail)
                    if (row.getMediaThumbnail().isPresent() && !row.getMediaThumbnail().get().isEmpty()) {
                        String thumbnailUrl = row.getMediaThumbnail().get();
                        
                        // Check if it's a Google Drive URL
                        if (thumbnailUrl.contains("drive.google.com")) {
                            // Build local file path: {groupName}/{unique_id}/thumbnail.jpg
                            String folderPath = String.format("%s/%s/%s",
                                    mediaStorePath,
                                    sanitizedGroup,
                                    sanitizedUniqueId);
                            String localThumbnailPath = folderPath + "/thumbnail.jpg";
                            
                            logger.debug("Downloading thumbnail - Group: " + groupName + ", UniqueId: " + uniqueId + 
                                       ", Path: " + localThumbnailPath);
                            
                            // Download thumbnail
                            boolean success = googleDriveDownloadService.downloadThumbnail(thumbnailUrl, localThumbnailPath);
                            
                            if (success) {
                                thumbnailsDownloaded++;
                                logger.info("Downloaded thumbnail " + thumbnailsDownloaded + "/" + rows.size() + 
                                           " - Group: " + groupName + ", UniqueId: " + uniqueId);
                                
                                // Update drill_item_import record with local thumbnail path
                                Optional<DrillItemImportRow> existingDrillItem = drillItemImportDao.findByUniqueId(uniqueId);
                                if (existingDrillItem.isPresent()) {
                                    drillItemImportDao.updateMediaThumbnail(existingDrillItem.get().getId(), localThumbnailPath, currentTime);
                                }
                            } else {
                                thumbnailsFailed++;
                                logger.error("Failed to download thumbnail for Group: " + groupName + 
                                           ", UniqueId: " + uniqueId + " from " + thumbnailUrl);
                            }
                            
                            // Pause between downloads
                            try {
                                Thread.sleep(DELAY_BETWEEN_DOWNLOADS_MS);
                            } catch (InterruptedException e) {
                                logger.warn("Download pause interrupted: " + e.getMessage());
                                Thread.currentThread().interrupt();
                                break;
                            }
                        } else {
                            logger.debug("Skipping non-Google Drive URL for media_thumbnail: " + thumbnailUrl);
                        }
                    }
                    
                    // Log progress periodically
                    if (rowsProcessed > 0 && rowsProcessed % PROGRESS_LOG_INTERVAL == 0) {
                        logger.info("Progress: Processed " + rowsProcessed + "/" + rows.size() + 
                                   " rows (Videos: " + videosDownloaded + " downloaded, " + videosFailed + " failed. " +
                                   "Thumbnails: " + thumbnailsDownloaded + " downloaded, " + thumbnailsFailed + " failed)");
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing row " + rowsProcessed + ": " + e.getMessage(), e);
                    // Continue processing other rows
                }
            }
            
            logger.info("Download completed - Processed " + rowsProcessed + " rows. " +
                       "Videos: " + videosDownloaded + " downloaded, " + videosFailed + " failed. " +
                       "Thumbnails: " + thumbnailsDownloaded + " downloaded, " + thumbnailsFailed + " failed");
            
            return new DownloadResult(videosDownloaded, videosFailed, thumbnailsDownloaded, thumbnailsFailed);
            
        } catch (Exception e) {
            logger.error("Error during CSV media download", e);
            throw new TransactionException(e);
        }
    }

    /**
     * Gets drill group name from drill_group_id by querying the database.
     * 
     * @param drillGroupId The drill group UUID
     * @return Group name or "Unknown" if not found
     */
    private String getGroupNameFromId(UUID drillGroupId) {
        try {
            // First try using DrillGroupConstants for known groups
            if (com.lektralabs.thrones.pallbearer.common.DrillGroupConstants.drillGroupIdNameMap.containsKey(drillGroupId)) {
                return com.lektralabs.thrones.pallbearer.common.DrillGroupConstants.drillGroupIdNameMap.get(drillGroupId);
            }
            
            // If not in constants, query the database
            String groupName = jdbiProvider.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT name FROM t_drill_group WHERE id = :drillGroupId")
                    .bind("drillGroupId", drillGroupId)
                    .mapTo(String.class)
                    .findOne()
                    .orElse(null)
            );
            
            return groupName != null ? groupName : "Unknown";
        } catch (Exception e) {
            logger.warn("Error getting group name for drill_group_id: " + drillGroupId + ", error: " + e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Scans the media folder structure and updates production tables with local file paths.
     * This method:
     * 1. Scans {mediaStorePath}/{groupName}/{unique_id}/ folders
     * 2. Matches folder names (case-insensitive) to unique_id in t_drill_item
     * 3. Verifies drill_group_id matches the group folder name
     * 4. Checks if both video.mp4 and thumbnail.jpg exist
     * 5. Updates t_drill_item.media_thumbnail with thumbnail path
     * 6. Updates t_media.content_url with video path
     * 
     * Folder structure expected: {mediaStorePath}/{groupName}/{unique_id}/video.mp4 and thumbnail.jpg
     * 
     * @return ScanResult containing update statistics
     */
    public ScanResult scanAndUpdateMediaPaths() {
        logger.info("Starting scan of media folder structure: " + mediaStorePath);
        
        int itemsScanned = 0;
        int itemsUpdated = 0;
        int itemsSkipped = 0;
        int itemsFailed = 0;
        long currentTime = System.currentTimeMillis();
        
        try {
            // Build map of group names (case-insensitive) to group IDs
            Map<String, UUID> groupNameToIdMap = buildGroupNameToIdMap();
            logger.info("Found " + groupNameToIdMap.size() + " drill groups to scan");
            
            // Get base media path
            Path mediaBasePath = Paths.get(mediaStorePath);
            if (!Files.exists(mediaBasePath) || !Files.isDirectory(mediaBasePath)) {
                logger.error("Media store path does not exist or is not a directory: " + mediaStorePath);
                return new ScanResult(0, 0, 0, 0, "Media store path does not exist");
            }
            
            // Scan each group folder
            File[] groupFolders = mediaBasePath.toFile().listFiles(File::isDirectory);
            if (groupFolders == null) {
                logger.warn("No group folders found in: " + mediaStorePath);
                return new ScanResult(0, 0, 0, 0, "No group folders found");
            }
            
            for (File groupFolder : groupFolders) {
                String groupFolderName = groupFolder.getName();
                
                // Find matching group ID (case-insensitive)
                UUID matchingGroupId = null;
                for (Map.Entry<String, UUID> entry : groupNameToIdMap.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(groupFolderName)) {
                        matchingGroupId = entry.getValue();
                        break;
                    }
                }
                
                if (matchingGroupId == null) {
                    logger.debug("Skipping folder (not a drill group): " + groupFolderName);
                    continue;
                }
                
                logger.info("Scanning group folder: " + groupFolderName + " (group_id: " + matchingGroupId + ")");
                
                // Scan subfolders (each should match a unique_id)
                File[] subfolders = groupFolder.listFiles(File::isDirectory);
                if (subfolders == null) {
                    logger.debug("No subfolders found in: " + groupFolderName);
                    continue;
                }
                
                for (File subfolder : subfolders) {
                    itemsScanned++;
                    String folderName = subfolder.getName();
                    
                    try {
                        // Find drill item by unique_id (case-insensitive)
                        Optional<com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> drillItemOpt = 
                            drillItemDao.findByUniqueId(folderName);
                        
                        if (!drillItemOpt.isPresent()) {
                            logger.debug("No drill item found for unique_id: " + folderName);
                            itemsSkipped++;
                            continue;
                        }
                        
                        com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName drillItem = drillItemOpt.get();
                        
                        // Verify drill_group_id matches
                        if (!drillItem.getDrillGroupId().equals(matchingGroupId)) {
                            logger.warn("Drill item unique_id: " + folderName + " belongs to different group. " +
                                       "Expected: " + matchingGroupId + ", Found: " + drillItem.getDrillGroupId());
                            itemsSkipped++;
                            continue;
                        }
                        
                        // Check if both video.mp4 and thumbnail.jpg exist
                        Path videoPath = subfolder.toPath().resolve("video.mp4");
                        Path thumbnailPath = subfolder.toPath().resolve("thumbnail.jpg");
                        
                        boolean videoExists = Files.exists(videoPath) && Files.isRegularFile(videoPath);
                        boolean thumbnailExists = Files.exists(thumbnailPath) && Files.isRegularFile(thumbnailPath);
                        
                        if (!videoExists && !thumbnailExists) {
                            logger.debug("Neither video.mp4 nor thumbnail.jpg found in: " + folderName);
                            itemsSkipped++;
                            continue;
                        }
                        
                        // Update thumbnail if it exists
                        if (thumbnailExists) {
                            String thumbnailAbsolutePath = thumbnailPath.toAbsolutePath().toString();
                            int updated = drillItemDao.updateMediaThumbnail(
                                drillItem.getId(), thumbnailAbsolutePath, currentTime);
                            if (updated > 0) {
                                logger.debug("Updated media_thumbnail for unique_id: " + folderName + 
                                           " -> " + thumbnailAbsolutePath);
                            }
                        }
                        
                        // Update video path in media table if it exists
                        if (videoExists && drillItem.getMediaId() != null && !drillItem.getMediaId().isEmpty()) {
                            try {
                                UUID mediaId = UUID.fromString(drillItem.getMediaId());
                                String videoAbsolutePath = videoPath.toAbsolutePath().toString();
                                int updated = mediaDao.updateContentUrl(mediaId, videoAbsolutePath, currentTime);
                                if (updated > 0) {
                                    logger.debug("Updated content_url for media_id: " + mediaId + 
                                               " (unique_id: " + folderName + ") -> " + videoAbsolutePath);
                                }
                            } catch (IllegalArgumentException e) {
                                logger.warn("Invalid media_id format for unique_id: " + folderName + 
                                          ", media_id: " + drillItem.getMediaId());
                            }
                        }
                        
                        itemsUpdated++;
                        
                        // Log progress periodically
                        if (itemsScanned > 0 && itemsScanned % 50 == 0) {
                            logger.info("Progress: Scanned " + itemsScanned + " folders, " + 
                                       itemsUpdated + " updated, " + itemsSkipped + " skipped, " + 
                                       itemsFailed + " failed");
                        }
                        
                    } catch (Exception e) {
                        itemsFailed++;
                        logger.error("Error processing folder: " + folderName + " in group: " + groupFolderName + 
                                   ", error: " + e.getMessage(), e);
                    }
                }
            }
            
            logger.info("Scan completed - Scanned: " + itemsScanned + " folders, " + 
                       itemsUpdated + " updated, " + itemsSkipped + " skipped, " + itemsFailed + " failed");
            
            return new ScanResult(itemsScanned, itemsUpdated, itemsSkipped, itemsFailed, null);
            
        } catch (Exception e) {
            logger.error("Error during folder scan", e);
            return new ScanResult(itemsScanned, itemsUpdated, itemsSkipped, itemsFailed, e.getMessage());
        }
    }

    /**
     * Builds a map of group names (case-insensitive key) to group IDs.
     * 
     * @return Map with lowercase group names as keys
     */
    private Map<String, UUID> buildGroupNameToIdMap() {
        Map<String, UUID> map = new HashMap<>();
        
        try {
            // First add known groups from constants
            for (Map.Entry<UUID, String> entry : 
                 com.lektralabs.thrones.pallbearer.common.DrillGroupConstants.drillGroupIdNameMap.entrySet()) {
                map.put(entry.getValue().toLowerCase(), entry.getKey());
            }
            
            // Then query database for any additional groups
            List<Map<String, Object>> allGroups = jdbiProvider.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT id, name FROM t_drill_group")
                    .mapToMap()
                    .list()
            );
            
            for (Map<String, Object> group : allGroups) {
                UUID groupId = (UUID) group.get("id");
                String groupName = (String) group.get("name");
                if (groupName != null) {
                    map.put(groupName.toLowerCase(), groupId);
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error building group name map: " + e.getMessage());
        }
        
        return map;
    }

    /**
     * Result object for scan operation
     */
    public static class ScanResult {
        private final int itemsScanned;
        private final int itemsUpdated;
        private final int itemsSkipped;
        private final int itemsFailed;
        private final String errorMessage;

        public ScanResult(int itemsScanned, int itemsUpdated, int itemsSkipped, int itemsFailed, String errorMessage) {
            this.itemsScanned = itemsScanned;
            this.itemsUpdated = itemsUpdated;
            this.itemsSkipped = itemsSkipped;
            this.itemsFailed = itemsFailed;
            this.errorMessage = errorMessage;
        }

        public int getItemsScanned() {
            return itemsScanned;
        }

        public int getItemsUpdated() {
            return itemsUpdated;
        }

        public int getItemsSkipped() {
            return itemsSkipped;
        }

        public int getItemsFailed() {
            return itemsFailed;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

