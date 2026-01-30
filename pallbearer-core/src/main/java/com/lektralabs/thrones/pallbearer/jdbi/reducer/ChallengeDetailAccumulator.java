package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.SportChallengeTypeDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ChallengeEntryItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.SportItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.ChallengeEntryItemKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.SportChallengeTypeKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.SportItemKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChallengeDetailAccumulator {

    final Map<UUID, ChallengeDetail> challengeDetailMap;

    final Map<SportChallengeTypeKey, SportChallengeTypeDetail> sportChallengeTypeDetailMap;

    final Map<SportItemKey, SportItem> sportItemMap;

    final Map<ChallengeEntryItemKey, ChallengeEntryItem> challengeEntryItemMap;

    final Map<UserDetailKey, UserDetail> createdByUserMap;

    final Map<UUID, ContactItem> createdByContactMap;

    final Map<UserDetailKey, UserDetail> modifiedByUserMap;

    final Map<UUID, ContactItem> modifiedByContactMap;

    public ChallengeDetailAccumulator() {
        this.challengeDetailMap = new HashMap<>();
        this.sportChallengeTypeDetailMap = new HashMap<>();
        this.sportItemMap = new HashMap<>();
        this.createdByUserMap = new HashMap<>();
        this.createdByContactMap = new HashMap<>();
        this.modifiedByUserMap = new HashMap<>();
        this.modifiedByContactMap = new HashMap<>();
        this.challengeEntryItemMap = new HashMap<>();
    }
}
