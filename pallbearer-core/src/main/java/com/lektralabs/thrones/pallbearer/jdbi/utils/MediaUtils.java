package com.lektralabs.thrones.pallbearer.jdbi.utils;

import java.io.File;
import java.nio.file.Files;

public interface MediaUtils {
    default byte[] getMediaBytes(String path) {
        byte[] empty = new byte[] {};
        File fh = new File(path);
        if  ( path.isEmpty() || !fh.exists() ) {
            return empty;
        } else {
            return getMediaBytes(fh);
        }
    }

    default byte[] getMediaBytes(File fh) {
        byte[] empty = new byte[] {};
        try {
            return Files.readAllBytes(fh.toPath());
        } catch (Exception e) {
            return empty;
        }
    }


    default String getMimeType(File file) {
        String UNKNOWN = "UNKNOWN";
        try {
            return Files.probeContentType(file.toPath());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

}
