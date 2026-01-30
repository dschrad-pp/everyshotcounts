package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AthleteDetailAccumulator {

    final Map<UUID, AthleteDetail> athleteDetailMap;

    public AthleteDetailAccumulator() {
        this.athleteDetailMap = new HashMap<>();
    }
}
