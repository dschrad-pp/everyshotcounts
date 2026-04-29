package com.lektralabs.thrones.pallbearer.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeLimitByLevelUpdateResponse {
    private String level;
    private List<Integer> levelIndexes;
    private Long timeLimitMs;
    private Boolean dryRun;
    private int matchedCount;
    private int updatedCount;
    private List<DrillItemTimeLimitChange> matchedDrills;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrillItemTimeLimitChange {
        private UUID drillItemId;
        private String name;
        private Integer levelIndex;
        private Long oldTimeLimitMs;
        private Long newTimeLimitMs;
    }
}
