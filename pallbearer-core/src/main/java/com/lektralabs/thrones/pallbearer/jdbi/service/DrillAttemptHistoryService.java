package com.lektralabs.thrones.pallbearer.jdbi.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillAttemptHistoryDao;
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

    public int getAttemptCount(UUID userId, UUID drillId) {
        return drillAttemptHistoryDao.getAttemptCount(userId, drillId);
    }
}
