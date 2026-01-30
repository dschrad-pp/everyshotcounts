package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.jdbi.service.RsAthleteMediaService;
import com.lektralabs.thrones.pallbearer.media.common.MediaConstants;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Path("/api/media/challenge")
public class ChallengeMediaResource {
    private static Logger logger = LoggerFactory.getLogger(ChallengeMediaResource.class);

    @Inject
    RsAthleteMediaService rsAthleteMediaService;

    @POST
    @Path("/{challengeId}/athlete/{athleteUserId}/video")
    @RolesAllowed({"ADMIN", "ATHLETE"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response athleteChallengeVideoCreate(@PathParam("challengeId") UUID challengeId,
                                                @PathParam("athleteUserId") UUID athleteUserId,
                                                @MultipartForm MultipartMediaResource upload) {
        try {
            logger.info("MediaResource received athlete challenge video upload" +
                            " with path: {} and name: {} and file size: {}" +
                            " from user {}",
                    upload.file.getAbsolutePath(),
                    upload.fileName,
                    Files.size(Paths.get(upload.file.getAbsolutePath())),
                    upload.createdById);
            return Response.ok(rsAthleteMediaService.createAthleteChallengeMedia(
                            challengeId, athleteUserId,
                            upload.fileName, upload.file))
                    .build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{challengeId}/athlete/{athleteUserId}/video.mp4")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaConstants.MP4_VIDEO_MIMETYPE)
    public Response athleteChallengeVideo(@PathParam("challengeId") UUID challengeId,
                                          @PathParam("athleteUserId") UUID athleteUserId) {
        logger.info("MediaResource received athlete challenge video request" +
                        " with challengeId: {} and athlete user ID: {}",
                challengeId,
                athleteUserId);
        try {
            byte[] bytes = rsAthleteMediaService.getAthleteChallengeVideo(challengeId, athleteUserId);
            if ( bytes.length > 0 ) {
                int length = bytes.length;
                int start = 0;
                int end = length - 1;
                return Response.ok(bytes)
                        .header("Accept-Ranges", "bytes")
                        .header("Content-Range", "bytes " + start + "-" + end + "/" + length)
                        .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch ( Exception e ) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{challengeId}/athlete/{athleteUserId}/highlightreel.mp4")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaConstants.MP4_VIDEO_MIMETYPE)
    public Response athleteChallengeHighlightReel(@PathParam("challengeId") UUID challengeId,
                                                  @PathParam("athleteUserId") UUID athleteUserId) {
        logger.info("MediaResource received athlete highlight video request" +
                        " with challengeId: {} and athlete user ID: {}",
                challengeId,
                athleteUserId);
        try {
            byte[] bytes = rsAthleteMediaService.getAthleteChallengeHighlightReel(challengeId, athleteUserId);
            int length = bytes.length;
            int start = 0;
            int end = length - 1;
            if ( bytes.length > 0 ) {
                return Response.ok(bytes)
                        .header("Accept-Ranges", "bytes")
                        .header("Content-Range", "bytes " + start + "-" + end + "/" + length)
                        .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch ( Exception e ) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{challengeId}/athlete/{athleteUserId}/stillframe")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaConstants.STILL_FRAME_MIMETYPE)
    public Response athleteChallengeVideoStillFrame(@PathParam("challengeId") UUID challengeId,
                                                    @PathParam("athleteUserId") UUID athleteUserId) {
        try {
            byte[] bytes = rsAthleteMediaService.getAthleteChallengeStillFrame(challengeId, athleteUserId);
            if ( bytes.length > 0 ) {
                return Response.ok(bytes).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch ( Exception e ) {
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/{challengeId}/athlete/{athleteUserId}/fan/{fanUserId}/video")
    @RolesAllowed({"ADMIN", "FAN"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response fanChallengeVideoCreate(@PathParam("challengeId") UUID challengeId,
                                            @PathParam("athleteUserId") UUID athleteUserId,
                                            @PathParam("fanUserId") UUID fanUserId,
                                            @MultipartForm MultipartMediaResource upload) {
        try {
            logger.info("MediaResource received fan challenge video upload" +
                            " with path: {} and name: {} and file size: {}" +
                            " from user {}",
                    upload.file.getAbsolutePath(),
                    upload.fileName,
                    Files.size(Paths.get(upload.file.getAbsolutePath())),
                    upload.createdById);
            return Response.ok(rsAthleteMediaService.createFanChallengeMedia(
                            challengeId, athleteUserId, fanUserId,
                            upload.fileName, upload.file))
                    .build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{challengeId}/athlete/{athleteUserId}/fan/{fanUserId}/stillframe")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaConstants.STILL_FRAME_MIMETYPE)
    public Response fanChallengeVideoStillFrame(@PathParam("challengeId") UUID challengeId,
                                                @PathParam("athleteUserId") UUID athleteUserId,
                                                @PathParam("fanUserId") UUID fanUserId) {
        try {
            byte[] bytes = rsAthleteMediaService.getFanChallengeStillFrame(challengeId, athleteUserId, fanUserId);
            if ( bytes.length > 0 ) {
                return Response.ok(bytes).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch ( Exception e ) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{challengeId}/athlete/{athleteUserId}/fan/{fanUserId}/mashup/video.mp4")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaConstants.MASHUP_MIMETYPE)
    public Response fanMashupVideo(@PathParam("challengeId") UUID challengeId,
                                   @PathParam("athleteUserId") UUID athleteUserId,
                                   @PathParam("fanUserId") UUID fanUserId) {
        logger.info("MediaResource received mashup video request" +
                        " with challengeId: {} and athlete user ID: {}" +
                        " and fan user ID: {}",
                challengeId,
                athleteUserId,
                fanUserId);
        try {
            byte[] bytes = rsAthleteMediaService.getFanMashupVideo(challengeId, athleteUserId, fanUserId);
            if ( bytes.length > 0 ) {
                return Response.ok(bytes).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch ( Exception e ) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{challengeId}/athlete/{athleteUserId}/fan/{fanUserId}/mashup/stillframe")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaConstants.STILL_FRAME_MIMETYPE)
    public Response fanMashupVideoStillFrame(@PathParam("challengeId") UUID challengeId,
                                             @PathParam("athleteUserId") UUID athleteUserId,
                                             @PathParam("fanUserId") UUID fanUserId) {
        try {
            byte[] bytes = rsAthleteMediaService.getFanMashupStillFrame(challengeId, athleteUserId, fanUserId);
            if ( bytes.length > 0 ) {
                return Response.ok(bytes).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch ( Exception e ) {
            return Response.serverError().build();
        }
    }

}
