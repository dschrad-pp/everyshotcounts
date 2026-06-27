package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One cell of the athlete activity heatmap: a calendar day (in the athlete's
 * requested timezone) and the number of drill completions submitted that day.
 *
 * <p>Counts are per completion submission, i.e. one row of
 * t_drill_attempt_history. Each submission carries a unique attempt_local_id and
 * is deduped at insert time (ON CONFLICT DO NOTHING), so this count reconciles
 * exactly with the iOS offline overlay.
 *
 * <p>Doubles as the JDBI bean-mapped row for {@code activityByDay} (columns
 * {@code date}, {@code completed_count}) and the API response item the client
 * consumes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDay {

    /** Local calendar day, formatted yyyy-MM-dd in the requested timezone. */
    private String date;

    /** Number of completion submissions bucketed to that local day. */
    private long completedCount;
}
