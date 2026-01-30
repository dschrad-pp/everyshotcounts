package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SportPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.SportBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SportRow;
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
public class SportBaseService {
    private static final Logger logger = Logger.getLogger(SportBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private SportBaseDao sportBaseDao;

    @PostConstruct
    public void init() {
        this.sportBaseDao = jdbiProvider.getJdbi().onDemand(SportBaseDao.class);
    }

    @Transactional
    public UUID create(SportPartial sportPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                SportRow sportRow = sportPartial.toRow(auditUser);
                return sportBaseDao.insert(sportRow);
            } catch (Exception e) {
                logger.warn("Error creating a [sport]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(SportPartial sportPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                SportRow sportRow = sportPartial.toRow(auditUser);
                return sportBaseDao.update(sportRow);
            } catch (Exception e) {
                logger.warn("Error updating a [sport]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<SportRow> findById(UUID id) {
        return sportBaseDao.findById(id);
    }

    public List<SportRow> findAll(FindOptions findOptions) {
        return sportBaseDao.findAll(findOptions.getSql());
    }

}
