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
public class TimeLimitByDrillItemsUpdateResponse {
    private Boolean dryRun;
    private int totalRequested;
    private int totalUpdated;
    private int totalFailed;
    private List<UpdatedDrillItem> updated;
    private List<FailedDrillItem> failed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatedDrillItem {
        private UUID drillItemId;
        private String name;
        private Long oldTimeLimitMs;
        private Long newTimeLimitMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedDrillItem {
        private UUID drillItemId;
        private Long requestedTimeLimitMs;
        private String reason;
    }
}
