package com.lektralabs.thrones.pallbearer.file;

import java.io.File;

public class FileUtils {

    public static String readFile(ClassLoader classLoader, String name) throws Exception {
        File file = new File(classLoader.getResource(name).getFile());
        return org.apache.commons.io.FileUtils.readFileToString(file, "UTF-8");
    }

}
