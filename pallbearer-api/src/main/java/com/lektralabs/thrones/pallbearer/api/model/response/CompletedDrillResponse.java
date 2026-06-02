package com.lektralabs.thrones.pallbearer.api.model.response;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TagRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletedDrillResponse {
    private String id;
    private String name;
    private Integer makesReported;
    private Integer attemptsReported;
    private List<TagRow> tags;
    private Integer bestMakeStreak;
    private Integer longestMissStreak;
    private Double avgTimePerRoundSeconds;
}
