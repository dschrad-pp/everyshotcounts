package com.lektralabs.thrones.pallbearer.jdbi.model.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeEntryItem {

    private UUID id;
    private UUID challengeId;
    private UUID tokenId;
    private UUID mediaId;
    private Integer score;
    private String statusCode;
    private Long creationDate;
    private UUID createdById;
}
