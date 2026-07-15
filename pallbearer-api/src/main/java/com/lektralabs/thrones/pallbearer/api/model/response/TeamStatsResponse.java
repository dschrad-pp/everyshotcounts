package com.lektralabs.thrones.pallbearer.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batched team-overview stats for the coach Team tab: one response replaces the
 * per-athlete snapshot fan-out (N+1) the client previously performed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamStatsResponse {

    private int athleteCount;

    /**
     * Mean of the per-athlete make percentages (athletes with at least one
     * session), rounded. Null when no athlete has any session yet — the client
     * renders a placeholder rather than a misleading 0%.
     */
    private Integer avgFgPercent;

    /** Sum of each athlete's active-group level order index. */
    private int levelsPassed;

    private List<AthleteStats> athletes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AthleteStats {
        private String athleteId;

        /** Overall make percentage; null when the athlete has no sessions yet. */
        private Integer fgPercent;

        private int sessionCount;
        private int totalMakes;
        private int totalAttempts;
        private int levelOrderIndex;
    }
}
