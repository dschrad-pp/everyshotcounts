package com.lektralabs.thrones.pallbearer.jdbi.model.generated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

// This is generated code. If you modify it (and you are welcome to), please
// move out of this package and remove this comment.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeRow implements Serializable {

    private UUID id;
    private Optional<String> name;
    private Optional<String> description;
    private Long startTime;
    private Long endTime;
    private UUID sportChallengeTypeId;
    private String challengeTypeCode;
    private String challengeVisibilityCode;
    private String allowRetryCode;
    private Integer retryMax;
    private String allowOpenInviteCode;
    private Long timeLimitMs;
    private Long creationDate;
    private Long modificationDate;
    private UUID createdById;
    private UUID modifiedById;
    private Integer version;
    private Optional<BigDecimal> costOfEntry;
    private Optional<Integer> numberOfShots;
    private Boolean personalizedIntro;
    private static final long serialVersionUID = 1L;

}