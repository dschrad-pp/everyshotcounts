package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsMemberActionPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.LeagueAppsMemberActionBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsMemberActionRow;
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
public class LeagueAppsMemberActionBaseService {
    private static final Logger logger = Logger.getLogger(LeagueAppsMemberActionBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private LeagueAppsMemberActionBaseDao leagueAppsMemberActionBaseDao;

    @PostConstruct
    public void init() {
        this.leagueAppsMemberActionBaseDao = jdbiProvider.getJdbi().onDemand(LeagueAppsMemberActionBaseDao.class);
    }

    @Transactional
    public long create(LeagueAppsMemberActionPartial leagueAppsMemberActionPartial) {
        long rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                LeagueAppsMemberActionRow leagueAppsMemberActionRow = leagueAppsMemberActionPartial.toRow(auditUser);
                return leagueAppsMemberActionBaseDao.insert(leagueAppsMemberActionRow);
            } catch (Exception e) {
                logger.warn("Error creating a [leagueAppsMemberAction]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(LeagueAppsMemberActionPartial leagueAppsMemberActionPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                LeagueAppsMemberActionRow leagueAppsMemberActionRow = leagueAppsMemberActionPartial.toRow(auditUser);
                return leagueAppsMemberActionBaseDao.update(leagueAppsMemberActionRow);
            } catch (Exception e) {
                logger.warn("Error updating a [leagueAppsMemberAction]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<LeagueAppsMemberActionRow> findById(long id) {
        return leagueAppsMemberActionBaseDao.findById(id);
    }

    public List<LeagueAppsMemberActionRow> findAll(FindOptions findOptions) {
        return leagueAppsMemberActionBaseDao.findAll(findOptions.getSql());
    }

}
