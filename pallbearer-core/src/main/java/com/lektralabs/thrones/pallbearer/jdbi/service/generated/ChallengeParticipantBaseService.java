package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeParticipantPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.ChallengeParticipantBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeParticipantRow;
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
public class ChallengeParticipantBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeParticipantBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private ChallengeParticipantBaseDao challengeParticipantBaseDao;

    @PostConstruct
    public void init() {
        this.challengeParticipantBaseDao = jdbiProvider.getJdbi().onDemand(ChallengeParticipantBaseDao.class);
    }

    @Transactional
    public UUID create(ChallengeParticipantPartial challengeParticipantPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeParticipantRow challengeParticipantRow = challengeParticipantPartial.toRow(auditUser);
                return challengeParticipantBaseDao.insert(challengeParticipantRow);
            } catch (Exception e) {
                logger.warn("Error creating a [challengeParticipant]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(ChallengeParticipantPartial challengeParticipantPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeParticipantRow challengeParticipantRow = challengeParticipantPartial.toRow(auditUser);
                return challengeParticipantBaseDao.update(challengeParticipantRow);
            } catch (Exception e) {
                logger.warn("Error updating a [challengeParticipant]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<ChallengeParticipantRow> findById(UUID id) {
        return challengeParticipantBaseDao.findById(id);
    }

    public List<ChallengeParticipantRow> findAll(FindOptions findOptions) {
        return challengeParticipantBaseDao.findAll(findOptions.getSql());
    }

}
