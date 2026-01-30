package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeTokenPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.ChallengeTokenBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeTokenRow;
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
public class ChallengeTokenBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeTokenBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private ChallengeTokenBaseDao challengeTokenBaseDao;

    @PostConstruct
    public void init() {
        this.challengeTokenBaseDao = jdbiProvider.getJdbi().onDemand(ChallengeTokenBaseDao.class);
    }

    @Transactional
    public UUID create(ChallengeTokenPartial challengeTokenPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeTokenRow challengeTokenRow = challengeTokenPartial.toRow(auditUser);
                return challengeTokenBaseDao.insert(challengeTokenRow);
            } catch (Exception e) {
                logger.warn("Error creating a [challengeToken]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(ChallengeTokenPartial challengeTokenPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeTokenRow challengeTokenRow = challengeTokenPartial.toRow(auditUser);
                return challengeTokenBaseDao.update(challengeTokenRow);
            } catch (Exception e) {
                logger.warn("Error updating a [challengeToken]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<ChallengeTokenRow> findById(UUID id) {
        return challengeTokenBaseDao.findById(id);
    }

    public List<ChallengeTokenRow> findAll(FindOptions findOptions) {
        return challengeTokenBaseDao.findAll(findOptions.getSql());
    }

}
