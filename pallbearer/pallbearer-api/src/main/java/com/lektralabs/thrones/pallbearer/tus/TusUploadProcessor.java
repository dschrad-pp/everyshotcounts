package com.lektralabs.thrones.pallbearer.tus;

import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TusUploadProcessor implements TusUploadUtils {

    @Inject
    TusUploadServiceFactory tusUploadServiceFactory;

    @Inject
    AthleteDrillService athleteDrillService;

    public void finalizeUpload(String requestUri, String ownerKey, String drillItemId, UUID userId) {
        TusFileUploadService tusFileUploadService = tusUploadServiceFactory.getTusFileUploadService();
        UploadInfo uploadInfo = null;
        try {
            uploadInfo = tusFileUploadService.getUploadInfo(requestUri, ownerKey);
        } catch (IOException | TusException e) {
            logger.error("get upload info", e);
        }

        if (uploadInfo != null && !uploadInfo.isUploadInProgress()) {
            try (InputStream is = tusFileUploadService.getUploadedBytes(requestUri, ownerKey)) {
                Path output = uploadDirectory.resolve(uploadInfo.getFileName());
                Files.copy(is, output, StandardCopyOption.REPLACE_EXISTING);
                UUID drillItemUuid = UUID.fromString(drillItemId);
                submitDrillVideo(drillItemUuid, userId, output);
            } catch (IOException | TusException e) {
                logger.error("get uploaded bytes", e);
            }

            try {
                tusFileUploadService.deleteUpload(requestUri);
            } catch (IOException | TusException e) {
                logger.error("delete upload", e);
            }
        }
    }

    public void submitDrillVideo(UUID drillItemId, UUID userId, Path output) {
        logger.info("Gallery resource received drill item, athlete drill" +
                        " submission video upload for drill item ID {}" +
                        " with path: {} and name: {}",
                drillItemId,
                output.toFile().getAbsolutePath(),
                output.getFileName());
        try {
            Optional<UUID> result = athleteDrillService.drillSubmission(
                    drillItemId, userId,
                    output.getFileName().toString(), output.toFile(),
                    0, 0);
        } catch (Exception e) {
            logger.error("Exception thrown submitting drill video for" +
                    " drill item ID={}", drillItemId, e);
            logger.error(e.getMessage());
        }
    }




}
