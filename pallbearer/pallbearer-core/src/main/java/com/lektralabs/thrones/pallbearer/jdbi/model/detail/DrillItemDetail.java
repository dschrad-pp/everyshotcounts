package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillItemDetail {

    private UUID id;

    private UUID teamId;

    private DrillGroupRow drillGroup;

    private String name;

    private String description;

    private Optional<UUID> mediaId;

    private Optional<String> mediaStatus;

    private Integer orderIndex;

    private Integer levelIndex;

    private Boolean levelTest;

    private Integer drillItemOrder;

    private Integer passingScore;

    private Integer shotsMax;

    private String visibilityCode;

    private String allowRetryCode;

    private Integer retryMax;

    private Long timeLimitMs;

    private Long creationDate;

    private Long modificationDate;

    private UserDetail createdByUserDetail;

    private UserDetail modifiedByUserDetail;

    private Integer version;

    private Optional<String> mediaThumbnail; // NEW FIELD

}
