package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeEntryPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.ChallengeEntryBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeEntryRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// This is generated code. Do not modify - it will be overwritten. See the extending class.

@ApplicationScoped
public class ChallengeEntryBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeEntryBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private ChallengeEntryBaseDao challengeEntryBaseDao;

    @PostConstruct
    public void init() {
        this.challengeEntryBaseDao = jdbiProvider.getJdbi().onDemand(ChallengeEntryBaseDao.class);
    }

    @Transactional
    public UUID create(ChallengeEntryPartial challengeEntryPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeEntryRow challengeEntryRow = challengeEntryPartial.toRow(auditUser);
                return challengeEntryBaseDao.insert(challengeEntryRow);
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
                ChallengeEntryRow challengeEntryRow = challengeEntryPartial.toRow(auditUser);
                return challengeEntryBaseDao.update(challengeEntryRow);
            } catch (Exception e) {
                logger.warn("Error updating a [challengeEntry]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<ChallengeEntryRow> findById(UUID id) {
        return challengeEntryBaseDao.findById(id);
    }

    public List<ChallengeEntryRow> findAll(FindOptions findOptions) {
        return challengeEntryBaseDao.findAll(findOptions.getSql());
    }

}
