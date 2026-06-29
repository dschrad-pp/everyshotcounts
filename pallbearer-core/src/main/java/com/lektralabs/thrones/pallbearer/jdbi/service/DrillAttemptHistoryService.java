package com.lektralabs.thrones.pallbearer.jdbi.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillAttemptHistoryDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ActivityDay;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptHistoryRow;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DrillAttemptHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(DrillAttemptHistoryService.class);

    private DrillAttemptHistoryDao drillAttemptHistoryDao;

    @Inject
    protected JdbiProvider jdbiProvider;

    @PostConstruct
    public void init() {
        this.drillAttemptHistoryDao = jdbiProvider.getJdbi().onDemand(DrillAttemptHistoryDao.class);
    }

    public List<DrillAttemptHistoryRow> findByDrillIdAndUserId(UUID drillId, UUID userId) {
        logger.debug("Fetching DrillAttemptHistory for drillId={} and userId={}", drillId, userId);
        return drillAttemptHistoryDao.findByDrillIdAndUserId(drillId, userId);
    }

    /**
     * Batched form of {@link #findByDrillIdAndUserId}: one round-trip for many drills
     * of a single user, grouped by drill id. Eliminates the per-drill N+1 fan-out on
     * the coach drill-detail / curriculum endpoints. Drills with no history are simply
     * absent from the map (callers use {@code getOrDefault(..., emptyList())}); the
     * per-drill ordering matches the single-drill query ({@code recorded_at DESC}).
     */
    public Map<UUID, List<DrillAttemptHistoryRow>> findByDrillIdsAndUserId(List<UUID> drillIds, UUID userId) {
        if (drillIds == null || drillIds.isEmpty()) {
            return Collections.emptyMap();
        }
        logger.debug("Batch-fetching DrillAttemptHistory for {} drillIds and userId={}", drillIds.size(), userId);
        return drillAttemptHistoryDao.findByDrillIdsAndUserId(drillIds, userId).stream()
                .collect(Collectors.groupingBy(DrillAttemptHistoryRow::getDrillId));
    }

    public UUID insertHistory(DrillAttemptHistoryRow row) {

        logger.debug("Inserting DrillAttemptHistoryRow: {}", row);
        return drillAttemptHistoryDao.insert(row);
    }

    public Optional<DrillAttemptHistoryRow> findById(UUID id) {
        logger.debug("Fetching DrillAttemptHistory by id={}", id);
        return drillAttemptHistoryDao.findById(id);
    }

    public List<DrillAttemptHistoryRow> findByDrillId(UUID drillId) {
        logger.debug("Fetching DrillAttemptHistory for drillId={}", drillId);
        return drillAttemptHistoryDao.findByDrillId(drillId);
    }

    public List<DrillAttemptHistoryRow> findByUserId(UUID userId) {
        logger.debug("Fetching DrillAttemptHistory for userId={}", userId);
        return drillAttemptHistoryDao.findByUserId(userId);
    }

    public int updateMediaId(UUID drillId, UUID mediaId, int version) {
        logger.debug("Updating mediaId for drillId={}, version={}", drillId, version);
        return drillAttemptHistoryDao.updateMediaId(drillId, mediaId, version);
    }

    public int updateLatestAttemptMediaId(UUID drillId, UUID mediaId) {
        logger.debug("Updating mediaId on latest attempt for drillId={}", drillId);
        return drillAttemptHistoryDao.updateLatestAttemptMediaId(drillId, mediaId);
    }

    public int updateMediaIdByAttemptLocalId(String attemptLocalId, UUID mediaId) {
        logger.debug("Updating mediaId for attemptLocalId={}", attemptLocalId);
        return drillAttemptHistoryDao.updateMediaIdByAttemptLocalId(attemptLocalId, mediaId);
    }

    public int getAttemptCount(UUID userId, UUID drillId) {
        return drillAttemptHistoryDao.getAttemptCount(userId, drillId);
    }

    /**
     * Per-day completion counts for the athlete activity heatmap, bucketed in the
     * given IANA timezone, both bounds inclusive. Zero-completion days are omitted.
     */
    public List<ActivityDay> getActivityByDay(UUID userId, LocalDate fromDate, LocalDate toDate, String tz) {
        logger.debug("Fetching activity heatmap for userId={} from {} to {} tz={}", userId, fromDate, toDate, tz);
        return drillAttemptHistoryDao.activityByDay(userId, fromDate, toDate, tz);
    }
}
