package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillAttemptPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.DrillAttemptBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptRow;
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
public class DrillAttemptBaseService {
    private static final Logger logger = Logger.getLogger(DrillAttemptBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private DrillAttemptBaseDao drillAttemptBaseDao;

    @PostConstruct
    public void init() {
        this.drillAttemptBaseDao = jdbiProvider.getJdbi().onDemand(DrillAttemptBaseDao.class);
    }

    @Transactional
    public UUID create(DrillAttemptPartial drillAttemptPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                DrillAttemptRow drillAttemptRow = drillAttemptPartial.toRow(auditUser);
                return drillAttemptBaseDao.insert(drillAttemptRow);
            } catch (Exception e) {
                logger.warn("Error creating a [drillAttempt]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(DrillAttemptPartial drillAttemptPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                DrillAttemptRow drillAttemptRow = drillAttemptPartial.toRow(auditUser);
                return drillAttemptBaseDao.update(drillAttemptRow);
            } catch (Exception e) {
                logger.warn("Error updating a [drillAttempt]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<DrillAttemptRow> findById(UUID id) {
        return drillAttemptBaseDao.findById(id);
    }

    public List<DrillAttemptRow> findAll(FindOptions findOptions) {
        return drillAttemptBaseDao.findAll(findOptions.getSql());
    }

}
