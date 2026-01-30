package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengePartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.ChallengeBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeRow;
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
public class ChallengeBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private ChallengeBaseDao challengeBaseDao;

    @PostConstruct
    public void init() {
        this.challengeBaseDao = jdbiProvider.getJdbi().onDemand(ChallengeBaseDao.class);
    }

    @Transactional
    public UUID create(ChallengePartial challengePartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeRow challengeRow = challengePartial.toRow(auditUser);
                return challengeBaseDao.insert(challengeRow);
            } catch (Exception e) {
                logger.warn("Error creating a [challenge]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(ChallengePartial challengePartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeRow challengeRow = challengePartial.toRow(auditUser);
                return challengeBaseDao.update(challengeRow);
            } catch (Exception e) {
                logger.warn("Error updating a [challenge]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<ChallengeRow> findById(UUID id) {
        return challengeBaseDao.findById(id);
    }

    public List<ChallengeRow> findAll(FindOptions findOptions) {
        return challengeBaseDao.findAll(findOptions.getSql());
    }

}
