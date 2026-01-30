package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.CommentPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.CommentBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.CommentRow;
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
public class CommentBaseService {
    private static final Logger logger = Logger.getLogger(CommentBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private CommentBaseDao commentBaseDao;

    @PostConstruct
    public void init() {
        this.commentBaseDao = jdbiProvider.getJdbi().onDemand(CommentBaseDao.class);
    }

    @Transactional
    public UUID create(CommentPartial commentPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                CommentRow commentRow = commentPartial.toRow(auditUser);
                return commentBaseDao.insert(commentRow);
            } catch (Exception e) {
                logger.warn("Error creating a [comment]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(CommentPartial commentPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                CommentRow commentRow = commentPartial.toRow(auditUser);
                return commentBaseDao.update(commentRow);
            } catch (Exception e) {
                logger.warn("Error updating a [comment]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<CommentRow> findById(UUID id) {
        return commentBaseDao.findById(id);
    }

    public List<CommentRow> findAll(FindOptions findOptions) {
        return commentBaseDao.findAll(findOptions.getSql());
    }

}
