package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.AddressPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.AddressBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.AddressRow;
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
public class AddressBaseService {
    private static final Logger logger = Logger.getLogger(AddressBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private AddressBaseDao addressBaseDao;

    @PostConstruct
    public void init() {
        this.addressBaseDao = jdbiProvider.getJdbi().onDemand(AddressBaseDao.class);
    }

    @Transactional
    public UUID create(AddressPartial addressPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                AddressRow addressRow = addressPartial.toRow(auditUser);
                return addressBaseDao.insert(addressRow);
            } catch (Exception e) {
                logger.warn("Error creating a [address]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(AddressPartial addressPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                AddressRow addressRow = addressPartial.toRow(auditUser);
                return addressBaseDao.update(addressRow);
            } catch (Exception e) {
                logger.warn("Error updating a [address]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<AddressRow> findById(UUID id) {
        return addressBaseDao.findById(id);
    }

    public List<AddressRow> findAll(FindOptions findOptions) {
        return addressBaseDao.findAll(findOptions.getSql());
    }

}
