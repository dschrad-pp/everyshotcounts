package com.lektralabs.thrones.pallbearer.api.model.request;

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
public class TimeLimitByDrillItemsUpdateRequest {
    private List<DrillItemTimeLimitUpdate> updates;
    private Boolean dryRun;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrillItemTimeLimitUpdate {
        private UUID drillItemId;
        private Long timeLimitMs;
    }
}
