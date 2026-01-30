package com.lektralabs.thrones.pallbearer.tus;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface TusUploadConstants {
    String UPLOAD_DIR = "/tmp/upload/tus";

    String NON_ID_URL = "/api/upload/tus/";

    String UPLOAD_URL = "/api/upload/tus/[0-9a-f\\-]+";

    Path uploadDirectory = Paths.get(UPLOAD_DIR);

}
