package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import com.lektralabs.thrones.pallbearer.jdbi.model.item.ChallengeEntryItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDetail {

    private UUID challengeId;

    private String name;

    private String description;

    private Long startTime;

    private Long endTime;

    private SportChallengeTypeDetail sportChallengeTypeDetail;

    private String challengeTypeCode;

    private String challengeVisibilityCode;

    private String allowRetryCode;

    private Integer retryMax;

    private String allowOpenInviteCode;

    private Long timeLimitMs;

    private BigDecimal costOfEntry;

    private Integer numberOfShots;

    private Boolean personalizedIntro;

    private Long creationDate;

    private Long modificationDate;

    private ChallengeEntryItem challengeEntryItem;

    private UserDetail createdByUserDetail;

    private UserDetail modifiedByUserDetail;
}
