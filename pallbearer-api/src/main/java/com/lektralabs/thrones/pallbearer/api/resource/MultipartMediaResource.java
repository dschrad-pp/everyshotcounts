package com.lektralabs.thrones.pallbearer.api.resource;

import jakarta.ws.rs.FormParam;

import java.io.File;

public class MultipartMediaResource {
    @FormParam("file")
    public File file;

    @FormParam("fileName")
    public String fileName;

    @FormParam("createdById")
    public String createdById;
}
