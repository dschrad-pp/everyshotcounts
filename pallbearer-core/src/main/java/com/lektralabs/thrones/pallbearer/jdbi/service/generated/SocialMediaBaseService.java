package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SocialMediaPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.SocialMediaBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SocialMediaRow;
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
public class SocialMediaBaseService {
    private static final Logger logger = Logger.getLogger(SocialMediaBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private SocialMediaBaseDao socialMediaBaseDao;

    @PostConstruct
    public void init() {
        this.socialMediaBaseDao = jdbiProvider.getJdbi().onDemand(SocialMediaBaseDao.class);
    }

    @Transactional
    public UUID create(SocialMediaPartial socialMediaPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                SocialMediaRow socialMediaRow = socialMediaPartial.toRow(auditUser);
                return socialMediaBaseDao.insert(socialMediaRow);
            } catch (Exception e) {
                logger.warn("Error creating a [socialMedia]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(SocialMediaPartial socialMediaPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                SocialMediaRow socialMediaRow = socialMediaPartial.toRow(auditUser);
                return socialMediaBaseDao.update(socialMediaRow);
            } catch (Exception e) {
                logger.warn("Error updating a [socialMedia]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<SocialMediaRow> findById(UUID id) {
        return socialMediaBaseDao.findById(id);
    }

    public List<SocialMediaRow> findAll(FindOptions findOptions) {
        return socialMediaBaseDao.findAll(findOptions.getSql());
    }

}
