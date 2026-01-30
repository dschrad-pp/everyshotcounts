package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.OrganizationPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.OrganizationBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.OrganizationRow;
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
public class OrganizationBaseService {
    private static final Logger logger = Logger.getLogger(OrganizationBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private OrganizationBaseDao organizationBaseDao;

    @PostConstruct
    public void init() {
        this.organizationBaseDao = jdbiProvider.getJdbi().onDemand(OrganizationBaseDao.class);
    }

    @Transactional
    public UUID create(OrganizationPartial organizationPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                OrganizationRow organizationRow = organizationPartial.toRow(auditUser);
                return organizationBaseDao.insert(organizationRow);
            } catch (Exception e) {
                logger.warn("Error creating a [organization]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(OrganizationPartial organizationPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                OrganizationRow organizationRow = organizationPartial.toRow(auditUser);
                return organizationBaseDao.update(organizationRow);
            } catch (Exception e) {
                logger.warn("Error updating a [organization]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<OrganizationRow> findById(UUID id) {
        return organizationBaseDao.findById(id);
    }

    public List<OrganizationRow> findAll(FindOptions findOptions) {
        return organizationBaseDao.findAll(findOptions.getSql());
    }

}
