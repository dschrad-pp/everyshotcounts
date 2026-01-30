package com.lektralabs.thrones.pallbearer.jdbi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillItemWithGroupName implements Serializable {

    private UUID id;
    private UUID drillGroupId;
    private String name;
    private String description;
    private String mediaId;
    private String mediaThumbnail;
    private Integer levelIndex;
    private Integer drillItemOrder;
    private Integer orderIndex;
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
    private String uniqueId;
    private String drillGroupName;

    private static final long serialVersionUID = 1L;
}
