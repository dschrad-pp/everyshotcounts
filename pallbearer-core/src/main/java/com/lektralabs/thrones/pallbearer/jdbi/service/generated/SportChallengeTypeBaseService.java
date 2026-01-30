package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SportChallengeTypePartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.SportChallengeTypeBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SportChallengeTypeRow;
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
public class SportChallengeTypeBaseService {
    private static final Logger logger = Logger.getLogger(SportChallengeTypeBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private SportChallengeTypeBaseDao sportChallengeTypeBaseDao;

    @PostConstruct
    public void init() {
        this.sportChallengeTypeBaseDao = jdbiProvider.getJdbi().onDemand(SportChallengeTypeBaseDao.class);
    }

    @Transactional
    public UUID create(SportChallengeTypePartial sportChallengeTypePartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                SportChallengeTypeRow sportChallengeTypeRow = sportChallengeTypePartial.toRow(auditUser);
                return sportChallengeTypeBaseDao.insert(sportChallengeTypeRow);
            } catch (Exception e) {
                logger.warn("Error creating a [sportChallengeType]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(SportChallengeTypePartial sportChallengeTypePartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                SportChallengeTypeRow sportChallengeTypeRow = sportChallengeTypePartial.toRow(auditUser);
                return sportChallengeTypeBaseDao.update(sportChallengeTypeRow);
            } catch (Exception e) {
                logger.warn("Error updating a [sportChallengeType]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<SportChallengeTypeRow> findById(UUID id) {
        return sportChallengeTypeBaseDao.findById(id);
    }

    public List<SportChallengeTypeRow> findAll(FindOptions findOptions) {
        return sportChallengeTypeBaseDao.findAll(findOptions.getSql());
    }

}
