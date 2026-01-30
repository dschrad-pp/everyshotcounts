package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsMemberPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.LeagueAppsMemberBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsMemberRow;
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
public class LeagueAppsMemberBaseService {
    private static final Logger logger = Logger.getLogger(LeagueAppsMemberBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private LeagueAppsMemberBaseDao leagueAppsMemberBaseDao;

    @PostConstruct
    public void init() {
        this.leagueAppsMemberBaseDao = jdbiProvider.getJdbi().onDemand(LeagueAppsMemberBaseDao.class);
    }

    @Transactional
    public long create(LeagueAppsMemberPartial leagueAppsMemberPartial) {
        long rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                LeagueAppsMemberRow leagueAppsMemberRow = leagueAppsMemberPartial.toRow(auditUser);
                return leagueAppsMemberBaseDao.insert(leagueAppsMemberRow);
            } catch (Exception e) {
                logger.warn("Error creating a [leagueAppsMember]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(LeagueAppsMemberPartial leagueAppsMemberPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                LeagueAppsMemberRow leagueAppsMemberRow = leagueAppsMemberPartial.toRow(auditUser);
                return leagueAppsMemberBaseDao.update(leagueAppsMemberRow);
            } catch (Exception e) {
                logger.warn("Error updating a [leagueAppsMember]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<LeagueAppsMemberRow> findById(long id) {
        return leagueAppsMemberBaseDao.findById(id);
    }

    public List<LeagueAppsMemberRow> findAll(FindOptions findOptions) {
        return leagueAppsMemberBaseDao.findAll(findOptions.getSql());
    }

}
