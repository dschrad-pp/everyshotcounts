package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LikePartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.LikeBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LikeRow;
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
public class LikeBaseService {
    private static final Logger logger = Logger.getLogger(LikeBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private LikeBaseDao likeBaseDao;

    @PostConstruct
    public void init() {
        this.likeBaseDao = jdbiProvider.getJdbi().onDemand(LikeBaseDao.class);
    }

    @Transactional
    public UUID create(LikePartial likePartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                LikeRow likeRow = likePartial.toRow(auditUser);
                return likeBaseDao.insert(likeRow);
            } catch (Exception e) {
                logger.warn("Error creating a [like]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(LikePartial likePartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                LikeRow likeRow = likePartial.toRow(auditUser);
                return likeBaseDao.update(likeRow);
            } catch (Exception e) {
                logger.warn("Error updating a [like]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<LikeRow> findById(UUID id) {
        return likeBaseDao.findById(id);
    }

    public List<LikeRow> findAll(FindOptions findOptions) {
        return likeBaseDao.findAll(findOptions.getSql());
    }

}
