package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TagRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillTagService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/drill-item-tag")
public class DrillItemTagResource {

    @Inject
    DrillTagService drillTagService;

    @GET
    @Path("/categories")
    @RolesAllowed({ "ADMIN", "COACH", "ATHLETE" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllTagCategories() {
        Map<?, List<TagRow>> categories = drillTagService.getAllTagCategoriesWithTags();
        return Response.ok(new GenericApiResponse<>(200, "Successfully fetched tag categories", categories)).build();
    }

    @GET
    @Path("/drill-item/{drillItemId}")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagsForDrillItem(@PathParam("drillItemId") UUID drillItemId) {
        List<TagRow> tags = drillTagService.getTagsForDrillItem(drillItemId);
        return Response.ok(new GenericApiResponse<>(200, "Successfully fetched tags", tags)).build();
    }

    @PUT
    @Path("/drill-item/{drillItemId}")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setTagsForDrillItem(@PathParam("drillItemId") UUID drillItemId, List<UUID> tagIds) {
        drillTagService.setTagsForDrillItem(drillItemId, tagIds);
        return Response.ok(new GenericApiResponse<>(200, "Tags updated successfully", null)).build();
    }

    @DELETE
    @Path("/drill-item/{drillItemId}/{tagId}")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeTagFromDrillItem(
            @PathParam("drillItemId") UUID drillItemId,
            @PathParam("tagId") UUID tagId) {
        drillTagService.removeTagFromDrillItem(drillItemId, tagId);
        return Response.ok(new GenericApiResponse<>(200, "Tag removed successfully", null)).build();
    }
}
