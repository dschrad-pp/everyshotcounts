package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.DrillBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lektralabs.thrones.pallbearer.common.DrillStatusConstants;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptHistoryRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillAttemptHistoryService;

// This is generated code. Do not modify - it will be overwritten. See the extending class.
@ApplicationScoped
public class DrillBaseService {

    private static final Logger logger = LoggerFactory.getLogger(DrillBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    @Inject
    protected DrillAttemptHistoryService drillAttemptHistoryService;

    private DrillBaseDao drillBaseDao;

    @PostConstruct
    public void init() {
        this.drillBaseDao = jdbiProvider.getJdbi().onDemand(DrillBaseDao.class);
    }

    @Transactional
    public UUID create(DrillPartial drillPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                DrillRow drillRow = drillPartial.toRow(auditUser);
                return drillBaseDao.insert(drillRow);
            } catch (Exception e) {
                logger.warn("Error creating a [drill]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

//applied retry mechanism due to failure during concurrent updates
    public int update(DrillPartial drillPartial, boolean incrementVersion) {
        final int maxRetries = 5; // Increased for stability under high concurrency
        int retries = 0;
        long backoffMillis = 100;

        while (true) {
            try {
                return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.READ_COMMITTED, handle -> {
                    Optional<DrillRow> currentDrillOpt = drillPartial.getDrillId()
                            .flatMap(drillBaseDao::findById);

                    if (currentDrillOpt.isEmpty()) {
                        throw new IllegalStateException("Drill not found for id: " + drillPartial.getDrillId().orElse(null));
                    }

                    DrillRow currentDrill = currentDrillOpt.get();
                    CurrentUser auditUser = userService.getCurrentUser();
                    DrillRow drillRow = drillPartial.toRow(auditUser);

                    Integer currentVersion = drillPartial.getVersion().orElseThrow(
                            () -> new IllegalStateException("Current version is required for update")
                    );
                    drillRow.setVersion(incrementVersion ? currentVersion + 1 : currentVersion);

                    int updateResult = drillBaseDao.update(drillRow, currentVersion);

                    if (updateResult == 0) {
                        throw new TransactionException("Optimistic lock failed: concurrent modification detected");
                    }

                    DrillRow updatedDrill = drillBaseDao.findById(drillPartial.getDrillId().get())
                            .orElseThrow(() -> new IllegalStateException("Drill not found after update"));

                    String newStatus = updatedDrill.getDrillStatus();
                    if (!DrillStatusConstants.NOT_ATTEMPTED.equals(newStatus) && !currentVersion.equals(updatedDrill.getVersion())) {
                        recordDrillHistory(updatedDrill,
                                "Drill updated with status=" + newStatus);
                    } else {
                        logger.info("Skipping attempt history recording for drillId={} due to status=NOT-ATTEMPTED",
                                updatedDrill.getId());
                    }

                    return updateResult;
                });
            } catch (TransactionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof org.postgresql.util.PSQLException
                        && "40001".equals(((org.postgresql.util.PSQLException) cause).getSQLState())
                        && retries < maxRetries) {
                    retries++;
                    logger.warn("Serialization failure on drill update for drillId={}. Retrying attempt {}/{} after {}ms",
                            drillPartial.getDrillId().orElse(null), retries, maxRetries, backoffMillis);
                    try {
                        Thread.sleep(backoffMillis);
                        backoffMillis *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    private void recordDrillHistory(DrillRow drill, String changeReason) {
        DrillAttemptHistoryRow historyRow = DrillAttemptHistoryRow.builder()
                .id(UUID.randomUUID())
                .drillId(drill.getId())
                .userId(drill.getUserId())
                .attemptsDetected(drill.getAttemptsDetected())
                .attemptsReported(drill.getAttemptsReported())
                .makesDetected(drill.getMakesDetected())
                .makesReported(drill.getMakesReported())
                .version(drill.getVersion())
                // attemptLocalId not available from DrillRow — left null so ON CONFLICT does not apply
                .attemptLocalId(null)
                .build();

        drillAttemptHistoryService.insertHistory(historyRow);
        logger.info("Recorded drill history for drillId={}, version={}, reason={}",
                drill.getId(), drill.getVersion(), changeReason);
    }

    public Optional<DrillRow> findById(UUID id) {
        return drillBaseDao.findById(id);
    }

    public List<DrillRow> findAll(FindOptions findOptions) {
        return drillBaseDao.findAll(findOptions.getSql());
    }

}
