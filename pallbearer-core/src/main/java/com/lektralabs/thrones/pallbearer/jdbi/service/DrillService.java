package com.lektralabs.thrones.pallbearer.jdbi.service;

import java.util.List;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillPartial;
import com.lektralabs.thrones.pallbearer.common.DrillStatusConstants;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.DrillBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.DrillBaseService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils; // Corrected import for DateTimeUtils

import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillAttemptHistoryResponse;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptHistoryRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.stream.Collectors;

@ApplicationScoped
public class DrillService extends DrillBaseService {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DrillWithHistory {

        private DrillRow drill;
        private List<DrillAttemptHistoryResponse> history;
    }

    private static Logger logger = LoggerFactory.getLogger(DrillService.class);

    private DrillDao drillDao;

    private DrillBaseDao drillBaseDao;

    @Inject
    DrillItemService drillItemService;

    @Inject
    DrillAttemptHistoryService drillAttemptHistoryService;

    @ConfigProperty(name = "pallbearer.server.base-url")
    String serverBaseUrl;

    @PostConstruct
    public void init() {
        super.init();
        this.drillDao = jdbiProvider.getJdbi().onDemand(DrillDao.class);
        this.drillBaseDao = jdbiProvider.getJdbi().onDemand(DrillBaseDao.class);
    }

    public Optional<DrillRow> findByDrillItemIdAndUserId(UUID drillItemId, UUID userId) {
        try {
            return drillDao.findByDrillItemIdAndUserId(drillItemId, userId);
        } catch (Exception e) {
            logger.info("error while fetching drill with drill item id and user id : " + e.getMessage());
            return null;
        }
    }

    public int updateMediaId(UUID drillId, UUID mediaId) {
        return drillDao.updateMediaId(drillId, mediaId);
    }

    public void updateAttemptMediaId(UUID drillId, UUID mediaId, int version) {
        drillAttemptHistoryService.updateMediaId(drillId, mediaId, version);
    }

    public void updateLatestAttemptMediaId(UUID drillId, UUID mediaId) {
        drillAttemptHistoryService.updateLatestAttemptMediaId(drillId, mediaId);
    }

    public int setDrillStatus(UUID drillId, String drillStatusCode) {
        return drillDao.updateDrillStatus(drillId, drillStatusCode);
    }

    public int updateAttemptsAndMakes(UUID drillId,
            int attempts, int makes) {
        return drillDao.updateAttemptsAndMakesDetected(drillId, attempts, makes);
    }

    public Optional<DrillWithHistory> findByIdWithHistory(UUID drillId) {
        return drillBaseDao.findById(drillId).map(drill -> {
            List<DrillAttemptHistoryRow> rows = drillAttemptHistoryService.findByDrillId(drillId);
            List<DrillAttemptHistoryResponse> history = rows.stream()
                    .map(r -> DrillAttemptHistoryResponse.from(r, serverBaseUrl))
                    .collect(Collectors.toList());
            return new DrillWithHistory(drill, history);
        });
    }

    /**
     * Wraps the base create method and applies sanity checks
     * <p>
     * </p>
     * A user can have zero or one drill submission for a drill item. If an
     * existing drill row exists, return it. Otherwise, create a new drill row
     * for the drill item and user
     *
     * @param drillPartial Drill partial
     * @return UUID of drill row
     */
    public UUID create(DrillPartial drillPartial) {
        UUID drillItemId = drillPartial.getDrillItemId();
        UUID athleteUserId = drillPartial.getUserId();

        Optional<DrillRow> maybeDrillRow = findByDrillItemIdAndUserId(
                drillItemId, athleteUserId);

        if (maybeDrillRow.isPresent()) {
            DrillRow drillRow = maybeDrillRow.get();
            logger.info("Drill service create request found existing"
                    + " drill for athlete ID={} and drill item ID={}"
                    + ". Skipping create and returning drill ID={}",
                    athleteUserId, drillItemId, drillRow.getId());
            return drillRow.getId();
        } else {
            if (drillPartial.getDrillStatus() == null) {
                drillPartial.setDrillStatus("NOT-ATTEMPTED");
            }
            return super.create(drillPartial);
        }
    }

    public UUID create(DrillPartial drillPartial, UUID auditUserId) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                // Manually build DrillRow using the provided auditUserId
                long now = DateTimeUtils.now().getMillis();
                DrillRow drillRow = DrillRow.builder()
                        .id(drillPartial.getDrillId().orElse(UUID.randomUUID()))
                        .drillItemId(drillPartial.getDrillItemId())
                        .userId(drillPartial.getUserId())
                        .mediaId(drillPartial.getMediaId())
                        .drillStatus(drillPartial.getDrillStatus() != null ? drillPartial.getDrillStatus() : "NOT-ATTEMPTED")
                        .creationDate(now)
                        .modificationDate(now)
                        .createdById(auditUserId) // Use the provided auditUserId
                        .modifiedById(auditUserId) // Use the provided auditUserId
                        .version(drillPartial.getVersion().orElse(0))
                        .attemptsDetected(drillPartial.getAttemptsDetected() != null ? drillPartial.getAttemptsDetected() : 0)
                        .attemptsReported(drillPartial.getAttemptsReported() != null ? drillPartial.getAttemptsReported() : 0)
                        .makesDetected(drillPartial.getMakesDetected() != null ? drillPartial.getMakesDetected() : 0)
                        .makesReported(drillPartial.getMakesReported() != null ? drillPartial.getMakesReported() : 0)
                        .build();
                return drillBaseDao.insert(drillRow);
            } catch (Exception e) {
                logger.warn("Error creating a [drill] with explicit auditUserId", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    /**
     * Put drill into retry mode. This sets the status to retry and unsets the
     * drill media
     *
     * @param drillId Drill row ID
     * @return 1 if success, zero if error
     */
    public int retryDrill(UUID drillId) {
        Optional<DrillRow> maybeDrillRow = drillBaseDao.findById(drillId);
        if (maybeDrillRow.isPresent()) {
            DrillRow drillRow = maybeDrillRow.get();
            DrillPartial drillPartial = DrillPartial.builder()
                    .drillId(Optional.of(drillRow.getId()))
                    .drillItemId(drillRow.getDrillItemId())
                    .userId(drillRow.getUserId())
                    .mediaId(Optional.empty())
                    .drillStatus(DrillStatusConstants.RETRY)
                    .version(Optional.of(drillRow.getVersion()))
                    .attemptsDetected(drillRow.getAttemptsDetected())
                    .attemptsReported(drillRow.getAttemptsReported())
                    .makesDetected(drillRow.getMakesDetected())
                    .makesReported(drillRow.getMakesReported())
                    .build();
            return update(drillPartial, true);
        } else {
            return 0;
        }
    }

    public List<DrillRow> findRecentDrillsForUser(UUID userId, int limit) {
        return drillDao.findRecentDrillsByUserId(userId, limit);
    }

    public List<DrillRow> findAllDrillsForUser(UUID userId) {
        return drillDao.findAllDrillsByUserId(userId);
    }

    public List<DrillRow> findAll() {
        // Create an empty FindOptions to represent fetching all items without specific filters
        FindOptions findOptions = new FindOptions();
        return super.findAll(findOptions);
    }
}
