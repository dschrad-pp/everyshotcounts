package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillGroupPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.DrillGroupBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
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
public class DrillGroupBaseService {
    private static final Logger logger = Logger.getLogger(DrillGroupBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private DrillGroupBaseDao drillGroupBaseDao;

    @PostConstruct
    public void init() {
        this.drillGroupBaseDao = jdbiProvider.getJdbi().onDemand(DrillGroupBaseDao.class);
    }

    @Transactional
    public UUID create(DrillGroupPartial drillGroupPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                DrillGroupRow drillGroupRow = drillGroupPartial.toRow(auditUser);
                return drillGroupBaseDao.insert(drillGroupRow);
            } catch (Exception e) {
                logger.warn("Error creating a [drillGroup]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(DrillGroupPartial drillGroupPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                DrillGroupRow drillGroupRow = drillGroupPartial.toRow(auditUser);
                return drillGroupBaseDao.update(drillGroupRow);
            } catch (Exception e) {
                logger.warn("Error updating a [drillGroup]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<DrillGroupRow> findById(UUID id) {
        return drillGroupBaseDao.findById(id);
    }

    public List<DrillGroupRow> findAll(FindOptions findOptions) {
        return drillGroupBaseDao.findAll(findOptions.getSql());
    }

}
