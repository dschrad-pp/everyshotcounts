package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeEntryPartial;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeTokenPartial;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.MediaPartial;
import com.lektralabs.thrones.pallbearer.common.MediaStatusConstants;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.MediaDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.MediaBaseService;
import com.lektralabs.thrones.pallbearer.jdbi.utils.MediaUtils;
import com.lektralabs.thrones.pallbearer.media.pipeline.realsports.FanMediaPipeline;
import com.lektralabs.thrones.pallbearer.media.pipeline.realsports.ChallengeMediaStore;
import com.lektralabs.thrones.pallbearer.media.pipeline.realsports.AthleteMediaPipeline;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RsAthleteMediaService extends MediaBaseService implements MediaUtils {

    private static final Logger logger = Logger.getLogger(MediaService.class);

    @Inject
    AthleteMediaPipeline athleteMediaPipeline;

    @Inject
    ChallengeEntryService challengeEntryService;

    @Inject
    ChallengeTokenService challengeTokenService;

    @Inject
    FanMediaPipeline fanMediaPipeline;

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    ChallengeMediaStore challengeMediaStore;

    @Inject
    UserService userService;

    private MediaDao mediaDao;

    @PostConstruct
    public void init() {
        super.init();
        // this.mediaDao = jdbiProvider.getJdbi().onDemand(MediaDao.class);
    }

    // TODO - sboles - move to an AthleteMediaService
    public UUID createAthleteChallengeMedia(UUID challengeId,
            UUID athleteUserId,
            String fileName,
            File videoFile) {

        ChallengeTokenPartial challengeTokenPartial = ChallengeTokenPartial.builder()
                .challengeId(challengeId)
                .userId(athleteUserId)
                .tokenStatusCode("ACTIVE")
                .tokenTypeCode("CHALLENGE")
                .build();
        UUID tokenId = challengeTokenService.create(challengeTokenPartial);

        byte empty[] = {};

        UUID mediaId = create(MediaPartial.builder()
                .mediaId(Optional.of(UUID.randomUUID()))
                .name(Optional.of(fileName))
                .description(Optional.of("Athlete challenge entry video"))
                .contentUrl(Optional.empty())
                .mimeType(Optional.of("video/mov"))
                .statusCode(MediaStatusConstants.COMPLETE)
                .build());

        ChallengeEntryPartial challengeEntryPartial = ChallengeEntryPartial.builder()
                .challengeEntryId(Optional.of(UUID.randomUUID()))
                .challengeId(challengeId)
                .tokenId(tokenId)
                .mediaId(mediaId)
                .score(1)
                .statusCode(StatusCode.PENDING.getValue())
                .build();

        UUID challengeEntryId = challengeEntryService.create(challengeEntryPartial);

        String result = athleteMediaPipeline.initializeChallengeMedia(
                challengeId, athleteUserId, challengeEntryId,
                fileName, videoFile);

        if (result.isEmpty()) {
            logger.error("Media Service failed to initialize media"
                    + " pipeline for challenge " + challengeId
                    + " with video file name " + fileName
                    + " at path " + videoFile.getAbsolutePath()
                    + ". See logs for details");
        }

        return challengeId;
    }

    public UUID createFanChallengeMedia(UUID challengeId,
            UUID athleteUserId,
            UUID fanUserId,
            String fileName,
            File videoFile) {

        // TODO - this probably belongs somewhere else
        UUID tokenId = challengeTokenService.create(ChallengeTokenPartial.builder()
                .challengeTokenId(Optional.of(UUID.randomUUID()))
                .challengeId(challengeId)
                .userId(fanUserId)
                .tokenStatusCode("ACTIVE")
                .tokenTypeCode("CHALLENGE_ENTRY")
                .build());

        byte arr[] = {};

        UUID mediaId = create(MediaPartial.builder()
                .mediaId(Optional.of(UUID.randomUUID()))
                .name(Optional.of(fileName))
                .description(Optional.of("Fan challenge entry video"))
                .contentUrl(Optional.empty())
                .mimeType(Optional.of("video/mov"))
                .statusCode(MediaStatusConstants.COMPLETE)
                .build());

        ChallengeEntryPartial challengeEntryPartial = ChallengeEntryPartial.builder()
                .challengeEntryId(Optional.of(UUID.randomUUID()))
                .challengeId(challengeId)
                .tokenId(tokenId)
                .mediaId(mediaId)
                .score(1)
                .statusCode(StatusCode.PENDING.getValue())
                .build();

        UUID challengeEntryId = challengeEntryService.create(challengeEntryPartial);

        String result = fanMediaPipeline.initializeChallengeMedia(
                challengeId, athleteUserId, fanUserId,
                challengeEntryId,
                fileName, videoFile);

        if (result.isEmpty()) {
            logger.error("Media Service failed to initialize media"
                    + " pipeline for challenge " + challengeId
                    + " with video file name " + fileName
                    + " at path " + videoFile.getAbsolutePath()
                    + ". See logs for errors");
        }

        return challengeId;
    }

    public byte[] getAthleteChallengeVideo(UUID challengeId,
            UUID athleteUserId) {
        String path = challengeMediaStore.getAthleteChallengeVideoPath(athleteUserId, challengeId);
        if (path.isEmpty()) {
            return new byte[]{};
        } else {
            return getMediaBytes(path);
        }
    }

    public byte[] getAthleteChallengeHighlightReel(UUID challengeId,
            UUID athleteUserId) {
        String path = challengeMediaStore.getAthleteChallengeHighlightReelPath(athleteUserId, challengeId);
        if (path.isEmpty()) {
            return new byte[]{};
        } else {
            return getMediaBytes(path);
        }
    }

    public byte[] getAthleteChallengeStillFrame(UUID challengeId,
            UUID athleteUserId) {
        String path = challengeMediaStore.getAthleteChallengeStillFramePath(athleteUserId, challengeId);
        if (path.isEmpty()) {
            return new byte[]{};
        } else {
            return getMediaBytes(path);
        }
    }

    public byte[] getFanChallengeStillFrame(UUID challengeId,
            UUID athleteUserId,
            UUID fanUserId) {
        String path = challengeMediaStore.getFanChallengeStillFramePath(athleteUserId, fanUserId, challengeId);
        if (path.isEmpty()) {
            return new byte[]{};
        } else {
            return getMediaBytes(path);
        }
    }

    public byte[] getFanMashupStillFrame(UUID challengeId,
            UUID athleteUserId,
            UUID fanUserId) {
        String path = challengeMediaStore.getFanMashupStillFramePath(athleteUserId, fanUserId, challengeId);
        if (path.isEmpty()) {
            return new byte[]{};
        } else {
            return getMediaBytes(path);
        }
    }

    public byte[] getFanMashupVideo(UUID challengeId,
            UUID athleteUserId,
            UUID fanUserId) {
        String path = challengeMediaStore.getFanMashupVideoPath(athleteUserId, fanUserId, challengeId);
        if (path.isEmpty()) {
            return new byte[]{};
        } else {
            return getMediaBytes(path);
        }
    }

}
