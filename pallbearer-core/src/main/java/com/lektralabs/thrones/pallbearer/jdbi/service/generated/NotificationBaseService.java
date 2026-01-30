package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.NotificationPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.NotificationBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.NotificationRow;
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
public class NotificationBaseService {
    private static final Logger logger = Logger.getLogger(NotificationBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private NotificationBaseDao notificationBaseDao;

    @PostConstruct
    public void init() {
        this.notificationBaseDao = jdbiProvider.getJdbi().onDemand(NotificationBaseDao.class);
    }

    @Transactional
    public UUID create(NotificationPartial notificationPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                NotificationRow notificationRow = notificationPartial.toRow(auditUser);
                return notificationBaseDao.insert(notificationRow);
            } catch (Exception e) {
                logger.warn("Error creating a [notification]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(NotificationPartial notificationPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                NotificationRow notificationRow = notificationPartial.toRow(auditUser);
                return notificationBaseDao.update(notificationRow);
            } catch (Exception e) {
                logger.warn("Error updating a [notification]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<NotificationRow> findById(UUID id) {
        return notificationBaseDao.findById(id);
    }

    public List<NotificationRow> findAll(FindOptions findOptions) {
        return notificationBaseDao.findAll(findOptions.getSql());
    }

}
