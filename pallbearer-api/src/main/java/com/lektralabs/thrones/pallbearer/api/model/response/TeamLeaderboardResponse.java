package com.lektralabs.thrones.pallbearer.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Athlete-facing team leaderboard: every athlete on the caller's team, ranked
 * best-FG-first, plus the same team-overview aggregates the coach Team tab
 * shows so both audiences read identical numbers. Served only to members of
 * the team (and admins) — leaving the team revokes access.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamLeaderboardResponse {

    private int athleteCount;

    /**
     * Mean of the per-athlete make percentages (athletes with at least one
     * session), rounded — same definition as {@link TeamStatsResponse} so the
     * coach and athlete screens can never disagree. Null when no athlete has
     * any session yet.
     */
    private Integer avgFgPercent;

    /** Sum of each athlete's active-group level order index. */
    private int levelsPassed;

    /** Ranked entries, best FG% first; athletes with no sessions sink to the bottom. */
    private List<Entry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {

        /** 1-based position after server-side ranking. */
        private int rank;

        private String athleteId;
        private String name;

        /** Overall make percentage; null when the athlete has no sessions yet. */
        private Integer fgPercent;

        private int sessionCount;
        private int levelOrderIndex;

        /**
         * ISO8601 UTC instant of the athlete's latest completion (e.g.
         * 2026-07-14T09:00:00Z); null when they have never submitted one.
         */
        private String lastActiveAt;
    }
}
