package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeParticipantDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChallengeParticipantDetailAccumulator {

    final Map<UUID, ChallengeParticipantDetail> challengeParticipantDetailMap;

    final Map<UserDetailKey, UserDetail> challengeParticipantUserMap;

    public ChallengeParticipantDetailAccumulator() {
        this.challengeParticipantDetailMap = new HashMap<>();
        this.challengeParticipantUserMap = new HashMap<>();
    }
}
