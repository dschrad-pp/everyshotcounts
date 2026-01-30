package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeEntryVotePartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.ChallengeEntryVoteBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeEntryVoteRow;
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
public class ChallengeEntryVoteBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeEntryVoteBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private ChallengeEntryVoteBaseDao challengeEntryVoteBaseDao;

    @PostConstruct
    public void init() {
        this.challengeEntryVoteBaseDao = jdbiProvider.getJdbi().onDemand(ChallengeEntryVoteBaseDao.class);
    }

    @Transactional
    public UUID create(ChallengeEntryVotePartial challengeEntryVotePartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeEntryVoteRow challengeEntryVoteRow = challengeEntryVotePartial.toRow(auditUser);
                return challengeEntryVoteBaseDao.insert(challengeEntryVoteRow);
            } catch (Exception e) {
                logger.warn("Error creating a [challengeEntryVote]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(ChallengeEntryVotePartial challengeEntryVotePartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeEntryVoteRow challengeEntryVoteRow = challengeEntryVotePartial.toRow(auditUser);
                return challengeEntryVoteBaseDao.update(challengeEntryVoteRow);
            } catch (Exception e) {
                logger.warn("Error updating a [challengeEntryVote]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<ChallengeEntryVoteRow> findById(UUID id) {
        return challengeEntryVoteBaseDao.findById(id);
    }

    public List<ChallengeEntryVoteRow> findAll(FindOptions findOptions) {
        return challengeEntryVoteBaseDao.findAll(findOptions.getSql());
    }

}
