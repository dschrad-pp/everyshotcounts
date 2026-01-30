package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.FinancialAccountPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.FinancialAccountBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.FinancialAccountRow;
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
public class FinancialAccountBaseService {
    private static final Logger logger = Logger.getLogger(FinancialAccountBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private FinancialAccountBaseDao financialAccountBaseDao;

    @PostConstruct
    public void init() {
        this.financialAccountBaseDao = jdbiProvider.getJdbi().onDemand(FinancialAccountBaseDao.class);
    }

    @Transactional
    public UUID create(FinancialAccountPartial financialAccountPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                FinancialAccountRow financialAccountRow = financialAccountPartial.toRow(auditUser);
                return financialAccountBaseDao.insert(financialAccountRow);
            } catch (Exception e) {
                logger.warn("Error creating a [financialAccount]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(FinancialAccountPartial financialAccountPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                FinancialAccountRow financialAccountRow = financialAccountPartial.toRow(auditUser);
                return financialAccountBaseDao.update(financialAccountRow);
            } catch (Exception e) {
                logger.warn("Error updating a [financialAccount]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<FinancialAccountRow> findById(UUID id) {
        return financialAccountBaseDao.findById(id);
    }

    public List<FinancialAccountRow> findAll(FindOptions findOptions) {
        return financialAccountBaseDao.findAll(findOptions.getSql());
    }

}
