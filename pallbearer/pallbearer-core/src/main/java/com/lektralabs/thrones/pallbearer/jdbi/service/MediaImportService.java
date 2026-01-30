package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.MediaImportDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.MediaImportRow;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MediaImportService {

    private static final Logger logger = Logger.getLogger(MediaImportService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private MediaImportDao mediaImportDao;

    @PostConstruct
    public void init() {
        this.mediaImportDao = jdbiProvider.getJdbi().onDemand(MediaImportDao.class);
    }

    @Transactional
    public int importFromCsv(InputStream csvInputStream) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                List<MediaImportRow> rows = parseCsv(csvInputStream, auditUser);
                
                if (rows.isEmpty()) {
                    logger.warn("No rows to import from CSV");
                    return 0;
                }

                for (MediaImportRow row : rows) {
                    mediaImportDao.insert(row);
                }
                
                logger.info("Successfully imported " + rows.size() + " media records from CSV");
                return rows.size();
            } catch (Exception e) {
                logger.error("Error importing CSV data", e);
                throw new TransactionException(e);
            }
        });
    }

    private List<MediaImportRow> parseCsv(InputStream csvInputStream, CurrentUser auditUser) throws Exception {
        List<MediaImportRow> rows = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        UUID defaultUserId = auditUser != null ? auditUser.getId() : UUID.fromString("d79ab826-65de-4fda-8b5f-779dacfe00fe");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvInputStream, StandardCharsets.UTF_8))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    MediaImportRow row = parseCsvLine(line, lineNumber, currentTime, defaultUserId);
                    rows.add(row);
                } catch (Exception e) {
                    logger.warn("Error parsing line " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        return rows;
    }

    private MediaImportRow parseCsvLine(String line, int lineNumber, long currentTime, UUID defaultUserId) {
        String[] values = parseCsvValues(line);
        
        if (values.length < 1) {
            throw new IllegalArgumentException("Line " + lineNumber + " does not have enough columns");
        }

        // CSV columns (in order):
        // name, description, content_url, status_code, mime_type
        
        int index = 0;
        
        String name = parseString(values, index++);
        String description = parseString(values, index++);
        String contentUrl = parseString(values, index++);
        String statusCode = parseString(values, index++, "ACTIVE");
        String mimeType = parseString(values, index++);

        return MediaImportRow.builder()
                .id(UUID.randomUUID())
                .name(Optional.ofNullable(name))
                .description(Optional.ofNullable(description))
                .contentUrl(Optional.ofNullable(contentUrl))
                .statusCode(statusCode)
                .mimeType(Optional.ofNullable(mimeType))
                .creationDate(currentTime)
                .modificationDate(currentTime)
                .createdById(defaultUserId)
                .modifiedById(defaultUserId)
                .version(0)
                .build();
    }

    private String[] parseCsvValues(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());
        
        return values.toArray(new String[0]);
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

    public Optional<MediaImportRow> findById(UUID id) {
        return mediaImportDao.findById(id);
    }

    public List<MediaImportRow> findAll() {
        return mediaImportDao.findAll("");
    }

    @Transactional
    public int deleteAll() {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            return mediaImportDao.deleteAll();
        });
    }
}
