package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptHistoryRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillAttemptHistoryResponse {

    private UUID id;
    private UUID userId;
    private UUID drillId;

    /**
     * The drill item (template) this round belongs to. The iOS client keys drill
     * cards off drillItemId, so it needs this to isolate a single item's rounds
     * from a name-unioned payload. Set by the caller, since the underlying
     * t_drill_attempt_history row only carries drill_id.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID drillItemId;

    private Integer attemptsDetected;
    private Integer attemptsReported;
    private Integer makesDetected;
    private Integer makesReported;
    private Timestamp recordedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp startedAt;
    private Integer version;
    private UUID mediaId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer hotStreak;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer coldStreak;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String videoUrl;

    public static DrillAttemptHistoryResponse from(DrillAttemptHistoryRow row, String serverBaseUrl) {
        String videoUrl = null;
        if (row.getMediaId() != null && serverBaseUrl != null) {
            String encodedPath;
            try {
                String relativePath = "gallery/" + row.getMediaId().toString() + "/source.mp4";
                encodedPath = java.net.URLEncoder.encode(relativePath, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20");
            } catch (Exception e) {
                encodedPath = "gallery/" + row.getMediaId().toString() + "/source.mp4";
            }
            videoUrl = serverBaseUrl + "/api/media/gallery/video?path=" + encodedPath;
        }

        return DrillAttemptHistoryResponse.builder()
                .id(row.getId())
                .userId(row.getUserId())
                .drillId(row.getDrillId())
                .attemptsDetected(row.getAttemptsDetected())
                .attemptsReported(row.getAttemptsReported())
                .makesDetected(row.getMakesDetected())
                .makesReported(row.getMakesReported())
                .recordedAt(row.getRecordedAt())
                .startedAt(row.getStartedAt())
                .version(row.getVersion())
                .mediaId(row.getMediaId())
                .hotStreak(row.getHotStreak())
                .coldStreak(row.getColdStreak())
                .videoUrl(videoUrl)
                .build();
    }
}
