package com.lektralabs.thrones.pallbearer.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeLimitByLevelUpdateRequest {
    private String level;
    private List<Integer> levelIndexes;
    private Long timeLimitMs;
    private Boolean dryRun;
}
