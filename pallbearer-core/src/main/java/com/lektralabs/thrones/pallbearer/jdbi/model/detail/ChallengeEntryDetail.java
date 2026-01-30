package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeEntryDetail {

    private UUID challengeEntryId;

    private UUID challengeId;

    private UUID tokenId;

    private UUID mediaId;

    private Integer score;

    private String statusCode;

    private Long creationDate;

    private Long modificationDate;

    private UserDetail createdByUserDetail;

    private UserDetail modifiedByUserDetail;
}

