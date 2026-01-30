package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.RolePartial;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.RoleDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.RoleRow;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoleService {
    private static final Logger logger = Logger.getLogger(RoleService.class);

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserService userService;

    private RoleDao roleDao;

    @PostConstruct
    public void init() {
        this.roleDao = jdbiProvider.getJdbi().onDemand(RoleDao.class);
    }

    @Transactional
    public UUID create(RolePartial rolePartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                RoleRow roleRow = rolePartial.toRow(auditUser);
                return roleDao.insert(roleRow);
            } catch (Exception e) {
                logger.warn("Error creating a [role]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(RolePartial rolePartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                RoleRow roleRow = rolePartial.toRow(auditUser);
                return roleDao.update(roleRow);
            } catch (Exception e) {
                logger.warn("Error updating a [role]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<RoleRow> findById(UUID id) {
        return roleDao.findById(id);
    }


}
