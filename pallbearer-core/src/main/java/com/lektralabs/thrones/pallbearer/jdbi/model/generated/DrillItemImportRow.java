package com.lektralabs.thrones.pallbearer.jdbi.model.generated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillItemImportRow implements Serializable {

    private UUID id;
    private UUID drillGroupId;
    private Optional<String> name;
    private Optional<String> description;
    private Optional<String> mediaId;  // Changed from UUID to String to accept URLs, paths, etc.
    private Optional<String> mediaThumbnail;

    private Optional<String> uniqueId;
    private Integer levelIndex;
    private Integer drillItemOrder;
    private Integer passingScore;
    private String visibilityCode;
    private String allowRetryCode;
    private Integer retryMax;
    private Long timeLimitMs;
    private Long creationDate;
    private Long modificationDate;
    private UUID createdById;
    private UUID modifiedById;
    private Integer version;
    private Boolean levelTest;
    private UUID teamId;
    private Integer shotsMax;
    private Integer orderIndex;

    private static final long serialVersionUID = 1L;

}

