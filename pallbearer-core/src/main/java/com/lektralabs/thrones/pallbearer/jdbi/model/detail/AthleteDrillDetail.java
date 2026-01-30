package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.MediaRow;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides a composite data model for an athlete drill that combines the drill
 * item definition with an optional athlete drill submission detail
 * <p />
 * The drill item may be locked to the athlete. Service level business logic
 * figures out the lock status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthleteDrillDetail {

    // drill item properties
    private UUID drillItemId;

    private UUID teamId;

    private Optional<String> name;

    private Optional<String> description;

    private Optional<UUID> mediaId;

    private Integer levelIndex;

    private Boolean levelTest;

    private Integer drillItemOrder;

    private Integer passingScore;

    private Integer shotsMax;

    private String visibilityCode;

    private String allowRetryCode;

    private Integer retryMax;

    private Long timeLimitMs;

    private Integer orderIndex;

    // drill item group
    private DrillGroupRow drillGroup;

    // media associated with the drill item
    private MediaRow media;

    // optional athlete drill submission
    private Optional<DrillDetail> drillDetail;

    // lock status
    private Boolean isLocked;

    private String mediaThumbnail;
}
