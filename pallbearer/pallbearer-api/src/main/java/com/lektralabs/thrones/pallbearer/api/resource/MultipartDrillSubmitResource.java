package com.lektralabs.thrones.pallbearer.api.resource;

import jakarta.ws.rs.FormParam;

import java.io.File;

public class MultipartDrillSubmitResource {

    @FormParam("file")
    public File file;

    @FormParam("fileName")
    public String fileName;

    @FormParam("createdById")
    public String createdById;

    @FormParam("attemptsReported")
    public String attemptsReported;

    @FormParam("makesReported")
    public String makesReported;
}
