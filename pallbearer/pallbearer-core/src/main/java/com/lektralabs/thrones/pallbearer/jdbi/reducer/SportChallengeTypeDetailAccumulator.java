package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.SportChallengeTypeDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.SportItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.SportItemKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SportChallengeTypeDetailAccumulator {

    final Map<UUID, SportChallengeTypeDetail> sportChallengeTypeDetailMap;
    final Map<SportItemKey, SportItem> sportItemMap;

    public SportChallengeTypeDetailAccumulator() {
        this.sportChallengeTypeDetailMap = new HashMap<>();
        this.sportItemMap = new HashMap<>();
    }
}
