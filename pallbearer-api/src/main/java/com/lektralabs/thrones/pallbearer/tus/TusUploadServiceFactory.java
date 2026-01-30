package com.lektralabs.thrones.pallbearer.tus;

import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import me.desair.tus.server.TusFileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class TusUploadServiceFactory implements TusUploadUtils {
    private static Logger logger = LoggerFactory.getLogger(TusUploadServiceFactory.class);

    private TusFileUploadService tusFileUploadService;

    @PostConstruct
    public void init() {
        ensureDirectoryExists(UPLOAD_DIR);
        tusFileUploadService = new TusFileUploadService()
                .withStoragePath(UPLOAD_DIR)
                .withUploadUri(UPLOAD_URL);
    }

    public TusFileUploadService getTusFileUploadService() {
        return tusFileUploadService;
    }

    // 1:15 am everyday
    @Scheduled(cron = "0 15 1 * * ?")
    protected void cleanup() {
        Path locksDir = uploadDirectory.resolve("locks");
        if (Files.exists(locksDir)) {
            try {
                tusFileUploadService.cleanup();
            } catch (IOException e) {
                logger.error("error during cleanup", e);
            }
        }
    }
}
