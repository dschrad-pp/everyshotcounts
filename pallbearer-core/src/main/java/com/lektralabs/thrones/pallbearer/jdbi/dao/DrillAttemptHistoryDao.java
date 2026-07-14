package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ActivityDay;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserLastActivity;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptHistoryRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(DrillAttemptHistoryRow.class)
public interface DrillAttemptHistoryDao {

    @UseStringTemplateSqlLocator
    @SqlUpdate("insertHistory")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean DrillAttemptHistoryRow historyRow);

    @UseStringTemplateSqlLocator
    @SqlQuery("findByDrillId")
    List<DrillAttemptHistoryRow> findByDrillId(@Bind("drillId") UUID drillId);

    @UseStringTemplateSqlLocator
    @SqlQuery("findByUserId")
    List<DrillAttemptHistoryRow> findByUserId(@Bind("userId") UUID userId);

    @UseStringTemplateSqlLocator
    @SqlQuery("findByDrillIdAndUserId")
    List<DrillAttemptHistoryRow> findByDrillIdAndUserId(@Bind("drillId") UUID drillId, @Bind("userId") UUID userId);

    /**
     * Batched sibling of {@link #findByDrillIdAndUserId}: fetch the attempt history
     * for many drills of a single user in one round-trip. Callers must guard against
     * an empty id list (the SQL would emit an invalid {@code IN ()}). Rows stay
     * ordered by {@code recorded_at DESC} so per-drill grouping preserves the same
     * ordering as the single-drill query.
     */
    @UseStringTemplateSqlLocator
    @SqlQuery("findByDrillIdsAndUserId")
    List<DrillAttemptHistoryRow> findByDrillIdsAndUserId(@BindList("drillIds") List<UUID> drillIds,
                                                         @Bind("userId") UUID userId);

    @UseStringTemplateSqlLocator
    @SqlQuery("findById")
    Optional<DrillAttemptHistoryRow> findById(@Bind("id") UUID id);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMediaId")
    int updateMediaId(@Bind("drillId") UUID drillId, @Bind("mediaId") UUID mediaId, @Bind("version") int version);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateLatestAttemptMediaId")
    int updateLatestAttemptMediaId(@Bind("drillId") UUID drillId, @Bind("mediaId") UUID mediaId);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMediaIdByAttemptLocalId")
    int updateMediaIdByAttemptLocalId(@Bind("attemptLocalId") String attemptLocalId, @Bind("mediaId") UUID mediaId);

    @UseStringTemplateSqlLocator
    @SqlQuery("getAttemptCount")
    int getAttemptCount(@Bind("userId") UUID userId, @Bind("drillId") UUID drillId);

    /**
     * Per-day completion counts for the athlete activity heatmap, bucketed in the
     * given IANA timezone. Both bounds inclusive. Days with zero completions are
     * absent from the result (the client zero-fills).
     */
    @UseStringTemplateSqlLocator
    @SqlQuery("activityByDay")
    @RegisterBeanMapper(ActivityDay.class)
    List<ActivityDay> activityByDay(@Bind("userId") UUID userId,
                                    @Bind("fromDate") LocalDate fromDate,
                                    @Bind("toDate") LocalDate toDate,
                                    @Bind("tz") String tz);

    /**
     * Latest completion timestamp per athlete, batched for the coach roster's
     * "last active" badge — one GROUP BY round-trip instead of a per-athlete
     * N+1. Callers must guard against an empty id list (the SQL would emit an
     * invalid {@code IN ()}). Athletes with no history are absent from the result.
     */
    @UseStringTemplateSqlLocator
    @SqlQuery("lastActivityByUserIds")
    @RegisterBeanMapper(UserLastActivity.class)
    List<UserLastActivity> lastActivityByUserIds(@BindList("userIds") List<UUID> userIds);
}
