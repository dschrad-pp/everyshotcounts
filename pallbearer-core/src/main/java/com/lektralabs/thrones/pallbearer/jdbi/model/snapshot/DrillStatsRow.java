package com.lektralabs.thrones.pallbearer.jdbi.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrillStatsRow {
    private UUID drillItemId;
    private String drillName;
    private int totalMakes;
    private int totalAttempts;
    private int sessions;
}
