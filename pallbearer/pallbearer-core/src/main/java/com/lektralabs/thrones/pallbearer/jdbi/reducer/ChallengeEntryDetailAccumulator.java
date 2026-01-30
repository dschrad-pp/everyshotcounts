package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeEntryDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChallengeEntryDetailAccumulator {

    final Map<UUID, ChallengeEntryDetail> challengeEntryDetailMap;

    final Map<UserDetailKey, UserDetail> createdByUserMap;

    final Map<UUID, ContactItem> createdByContactMap;

    final Map<UserDetailKey, UserDetail> modifiedByUserMap;

    final Map<UUID, ContactItem> modifiedByContactMap;

    public ChallengeEntryDetailAccumulator() {
        this.challengeEntryDetailMap = new HashMap<>();
        this.createdByUserMap = new HashMap<>();
        this.createdByContactMap = new HashMap<>();
        this.modifiedByUserMap = new HashMap<>();
        this.modifiedByContactMap = new HashMap<>();
    }
}

