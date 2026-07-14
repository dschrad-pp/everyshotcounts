package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * JDBI bean-mapped row for {@code lastActivityByUserIds}: an athlete and the
 * timestamp of their most recent completion submission (one row of
 * t_drill_attempt_history), pre-formatted as an ISO8601 UTC instant string.
 * Feeds the coach roster's {@code lastActiveAt} field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLastActivity {

    private UUID userId;

    /** ISO8601 UTC instant of the athlete's latest completion, e.g. 2026-07-14T09:00:00Z. */
    private String lastActiveAt;
}
