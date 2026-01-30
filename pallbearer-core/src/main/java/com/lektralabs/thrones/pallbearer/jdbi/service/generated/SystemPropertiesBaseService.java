package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SystemPropertiesPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.SystemPropertiesBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SystemPropertiesRow;
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
public class SystemPropertiesBaseService {
    private static final Logger logger = Logger.getLogger(SystemPropertiesBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private SystemPropertiesBaseDao systemPropertiesBaseDao;

    @PostConstruct
    public void init() {
        this.systemPropertiesBaseDao = jdbiProvider.getJdbi().onDemand(SystemPropertiesBaseDao.class);
    }

    @Transactional
    public UUID create(SystemPropertiesPartial systemPropertiesPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                SystemPropertiesRow systemPropertiesRow = systemPropertiesPartial.toRow(auditUser);
                return systemPropertiesBaseDao.insert(systemPropertiesRow);
            } catch (Exception e) {
                logger.warn("Error creating a [systemProperties]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(SystemPropertiesPartial systemPropertiesPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                SystemPropertiesRow systemPropertiesRow = systemPropertiesPartial.toRow(auditUser);
                return systemPropertiesBaseDao.update(systemPropertiesRow);
            } catch (Exception e) {
                logger.warn("Error updating a [systemProperties]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<SystemPropertiesRow> findById(UUID id) {
        return systemPropertiesBaseDao.findById(id);
    }

    public List<SystemPropertiesRow> findAll(FindOptions findOptions) {
        return systemPropertiesBaseDao.findAll(findOptions.getSql());
    }

}
