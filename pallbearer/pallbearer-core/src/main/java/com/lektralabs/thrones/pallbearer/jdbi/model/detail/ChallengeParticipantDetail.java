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
public class ChallengeParticipantDetail {

    private UUID challengeParticipantId;

    private UUID challengeId;

    private UserDetail participantDetail;

    private String participantRoleCode;
}
