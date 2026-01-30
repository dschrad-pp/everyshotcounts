package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ContactPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.ContactBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ContactRow;
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
public class ContactBaseService {
    private static final Logger logger = Logger.getLogger(ContactBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private ContactBaseDao contactBaseDao;

    @PostConstruct
    public void init() {
        this.contactBaseDao = jdbiProvider.getJdbi().onDemand(ContactBaseDao.class);
    }

    @Transactional
    public UUID create(ContactPartial contactPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ContactRow contactRow = contactPartial.toRow(auditUser);
                return contactBaseDao.insert(contactRow);
            } catch (Exception e) {
                logger.warn("Error creating a [contact]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(ContactPartial contactPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                ContactRow contactRow = contactPartial.toRow(auditUser);
                return contactBaseDao.update(contactRow);
            } catch (Exception e) {
                logger.warn("Error updating a [contact]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<ContactRow> findById(UUID id) {
        return contactBaseDao.findById(id);
    }

    public List<ContactRow> findAll(FindOptions findOptions) {
        return contactBaseDao.findAll(findOptions.getSql());
    }

}
