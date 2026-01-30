package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.MediaPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.MediaBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.MediaRow;
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
public class MediaBaseService {
    private static final Logger logger = Logger.getLogger(MediaBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private MediaBaseDao mediaBaseDao;

    @PostConstruct
    public void init() {
        this.mediaBaseDao = jdbiProvider.getJdbi().onDemand(MediaBaseDao.class);
    }

    @Transactional
    public UUID create(MediaPartial mediaPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                MediaRow mediaRow = mediaPartial.toRow(auditUser);
                return mediaBaseDao.insert(mediaRow);
            } catch (Exception e) {
                logger.warn("Error creating a [media]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(MediaPartial mediaPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                MediaRow mediaRow = mediaPartial.toRow(auditUser);
                return mediaBaseDao.update(mediaRow);
            } catch (Exception e) {
                logger.warn("Error updating a [media]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<MediaRow> findById(UUID id) {
        return mediaBaseDao.findById(id);
    }

    public List<MediaRow> findAll(FindOptions findOptions) {
        return mediaBaseDao.findAll(findOptions.getSql());
    }

}
