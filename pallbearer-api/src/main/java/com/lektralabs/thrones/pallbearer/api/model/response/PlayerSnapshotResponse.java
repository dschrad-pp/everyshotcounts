package com.lektralabs.thrones.pallbearer.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSnapshotResponse {

    private String id;
    private String name;
    private String levelLabel;
    private int sessionCount;
    private int overallMakePercent;
    private int makePercent;
    private int totalMakes;
    private int totalAttempts;
    private int bestSessionMakes;
    private int worstSessionMisses;
    private int bestMakeStreak;
    private int worstMissStreak;
    private int roundsToPass;
    private int levelProgress;
    private List<SkillBreakdown> skillBreakdown;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillBreakdown {
        private String tagCode;
        private int coveragePercent;
    }
}
