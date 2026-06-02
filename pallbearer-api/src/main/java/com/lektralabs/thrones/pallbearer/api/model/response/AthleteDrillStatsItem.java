package com.lektralabs.thrones.pallbearer.api.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthleteDrillStatsItem {

    private String id;
    private String name;
    private int makePercent;
    private int totalMakes;
    private int totalAttempts;
    private int sessions;
    private int bestMakeStreak;
    private int longestMissStreak;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double avgTimePerRoundSeconds;
    private List<DrillTag> tags;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DrillTag {
        private String code;
        private String name;
    }
}
