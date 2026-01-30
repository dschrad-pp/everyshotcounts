package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.jdbi.service.MediaImportService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/media_import")
public class MediaImportResource {

    private static Logger logger = LoggerFactory.getLogger(MediaImportResource.class);

    @Inject
    MediaImportService mediaImportService;

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "COACH"})
    public Response importCsv(MultipartFormDataInput input) {
        try {
            Map<String, List<InputPart>> formParts = input.getFormDataMap();
            List<InputPart> fileParts = formParts.get("file");

            if (fileParts == null || fileParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"No file provided\"}")
                        .build();
            }

            InputPart filePart = fileParts.get(0);
            InputStream fileInputStream = filePart.getBody(InputStream.class, null);

            int importedCount = mediaImportService.importFromCsv(fileInputStream);

            return Response.ok()
                    .entity("{\"message\": \"Successfully imported " + importedCount + " records\", \"count\": " + importedCount + "}")
                    .build();
        } catch (Exception e) {
            logger.error("Error importing CSV file", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMediaImport(@PathParam("id") UUID id) {
        return mediaImportService.findById(id)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMediaImports() {
        return Response.ok(mediaImportService.findAll()).build();
    }

    @DELETE
    @Path("/")
    @RolesAllowed({"ADMIN"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAll() {
        int deletedCount = mediaImportService.deleteAll();
        return Response.ok()
                .entity("{\"message\": \"Deleted " + deletedCount + " records\", \"count\": " + deletedCount + "}")
                .build();
    }
}
