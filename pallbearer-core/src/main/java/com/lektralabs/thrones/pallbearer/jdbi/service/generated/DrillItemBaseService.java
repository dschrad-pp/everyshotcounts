package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillItemPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.DrillItemBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
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
public class DrillItemBaseService {
    private static final Logger logger = Logger.getLogger(DrillItemBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private DrillItemBaseDao drillItemBaseDao;

    @PostConstruct
    public void init() {
        this.drillItemBaseDao = jdbiProvider.getJdbi().onDemand(DrillItemBaseDao.class);
    }

    @Transactional
    public UUID create(DrillItemPartial drillItemPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                DrillItemRow drillItemRow = drillItemPartial.toRow(auditUser);
                return drillItemBaseDao.insert(drillItemRow);
            } catch (Exception e) {
                logger.warn("Error creating a [drillItem]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(DrillItemPartial drillItemPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                DrillItemRow drillItemRow = drillItemPartial.toRow(auditUser);
                return drillItemBaseDao.update(drillItemRow);
            } catch (Exception e) {
                logger.warn("Error updating a [drillItem]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<DrillItemRow> findById(UUID id) {
        return drillItemBaseDao.findById(id);
    }

    public List<DrillItemRow> findAll(FindOptions findOptions) {
        return drillItemBaseDao.findAll(findOptions.getSql());
    }

}
