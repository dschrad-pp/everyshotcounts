package com.lektralabs.thrones.pallbearer.tus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface TusUploadUtils extends TusUploadConstants {
    Logger logger = LoggerFactory.getLogger(TusUploadUtils.class);

    Pattern REQUEST_URI_REGEX = Pattern.compile("^.*?/api/upload/tus/([a-z0-9\\-]{36})/?.*?$", Pattern.CASE_INSENSITIVE);

    default String getUuid(String requestUri) {
        Matcher matcher = REQUEST_URI_REGEX.matcher(requestUri);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            logger.error("No match found for [%s]".formatted(requestUri));
            return null;
        }
    }

    default String getOwnerKey(String drillItemId, UUID userId) {
        return "%s__%s".formatted(drillItemId, userId.toString());
    }

    default void ensureDirectoryExists(String dir) {
        File uploadDir = new File(dir);
        if (!uploadDir.exists()) {
            boolean rv = uploadDir.mkdirs();
            if (!rv) {
                logger.error("Could not create upload directory [%s]".formatted(dir));
            }
        }
    }





}
