package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeEntryPartial;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.ChallengeEntryDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeEntryRow;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// This is generated code. If you modify it (and you are welcome to), please
// move out of this package and remove this comment.

@ApplicationScoped
public class ChallengeEntryService {
    private static final Logger logger = Logger.getLogger(ChallengeEntryService.class);

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserService userService;

    private ChallengeEntryDao challengeEntryDao;

    @PostConstruct
    public void init() {
        this.challengeEntryDao = jdbiProvider.getJdbi().onDemand(ChallengeEntryDao.class);
    }

    @Transactional
    public UUID create(ChallengeEntryPartial challengeEntryPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeEntryRow challengeEntryRow = challengeEntryPartial.toRow(auditUser);
                return challengeEntryDao.insert(challengeEntryRow);
            } catch (Exception e) {
                logger.warn("Error creating a [challengeEntry]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(ChallengeEntryPartial challengeEntryPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                // TODO - sboles, kbrumer - this has possible unintended side
                // effects like overwriting the creation user and date
                ChallengeEntryRow challengeEntryRow = challengeEntryPartial.toRow(auditUser);
                return challengeEntryDao.update(challengeEntryRow);
            } catch (Exception e) {
                logger.warn("Error updating a [challengeEntry]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(ChallengeEntryRow challengeEntryRow) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                challengeEntryRow.setModificationDate(DateTimeUtils.now().getMillis());
                return challengeEntryDao.update(challengeEntryRow);
            } catch (Exception e) {
                logger.warn("Error updating a [challengeEntry]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public UUID create(UUID challengeId, UUID tokenId, UUID mediaId) {
        return create(
                ChallengeEntryPartial.builder()
                        .challengeEntryId(Optional.of(UUID.randomUUID()))
                        .challengeId(challengeId)
                        .tokenId(tokenId)
                        .mediaId(mediaId)
                        .score(1)
                        .statusCode(StatusCode.PENDING.getValue())
                        .build()
        );
    }

    public Optional<ChallengeEntryRow> findById(UUID id) {
        return challengeEntryDao.findById(id);
    }

    public List<ChallengeEntryRow> findByChallenge(UUID challengeId) {
        return challengeEntryDao.findByChallenge(challengeId);
    }

    public Optional<ChallengeEntryRow> findByChallengeAndUser(UUID challengeId,
                                                              UUID userId) {
        return challengeEntryDao.findByChallengeAndUser(challengeId, userId);
    }

    public Optional<Integer> activate(UUID challengeEntryId) {
        return findById(challengeEntryId).map(row -> {
            row.setStatusCode(StatusCode.ACTIVE.getValue());
            return update(row);
        });
    }
}
