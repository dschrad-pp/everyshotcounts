package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeInvitationPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.ChallengeInvitationBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeInvitationRow;
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
public class ChallengeInvitationBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeInvitationBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private ChallengeInvitationBaseDao challengeInvitationBaseDao;

    @PostConstruct
    public void init() {
        this.challengeInvitationBaseDao = jdbiProvider.getJdbi().onDemand(ChallengeInvitationBaseDao.class);
    }

    @Transactional
    public UUID create(ChallengeInvitationPartial challengeInvitationPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeInvitationRow challengeInvitationRow = challengeInvitationPartial.toRow(auditUser);
                return challengeInvitationBaseDao.insert(challengeInvitationRow);
            } catch (Exception e) {
                logger.warn("Error creating a [challengeInvitation]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(ChallengeInvitationPartial challengeInvitationPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ChallengeInvitationRow challengeInvitationRow = challengeInvitationPartial.toRow(auditUser);
                return challengeInvitationBaseDao.update(challengeInvitationRow);
            } catch (Exception e) {
                logger.warn("Error updating a [challengeInvitation]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<ChallengeInvitationRow> findById(UUID id) {
        return challengeInvitationBaseDao.findById(id);
    }

    public List<ChallengeInvitationRow> findAll(FindOptions findOptions) {
        return challengeInvitationBaseDao.findAll(findOptions.getSql());
    }

}
