package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsRegistrationPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.LeagueAppsRegistrationBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsRegistrationRow;
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
public class LeagueAppsRegistrationBaseService {
    private static final Logger logger = Logger.getLogger(LeagueAppsRegistrationBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private LeagueAppsRegistrationBaseDao leagueAppsRegistrationBaseDao;

    @PostConstruct
    public void init() {
        this.leagueAppsRegistrationBaseDao = jdbiProvider.getJdbi().onDemand(LeagueAppsRegistrationBaseDao.class);
    }

    @Transactional
    public long create(LeagueAppsRegistrationPartial leagueAppsRegistrationPartial) {
        long rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                LeagueAppsRegistrationRow leagueAppsRegistrationRow = leagueAppsRegistrationPartial.toRow(auditUser);
                return leagueAppsRegistrationBaseDao.insert(leagueAppsRegistrationRow);
            } catch (Exception e) {
                logger.warn("Error creating a [leagueAppsRegistration]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(LeagueAppsRegistrationPartial leagueAppsRegistrationPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                LeagueAppsRegistrationRow leagueAppsRegistrationRow = leagueAppsRegistrationPartial.toRow(auditUser);
                return leagueAppsRegistrationBaseDao.update(leagueAppsRegistrationRow);
            } catch (Exception e) {
                logger.warn("Error updating a [leagueAppsRegistration]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<LeagueAppsRegistrationRow> findById(long id) {
        return leagueAppsRegistrationBaseDao.findById(id);
    }

    public List<LeagueAppsRegistrationRow> findAll(FindOptions findOptions) {
        return leagueAppsRegistrationBaseDao.findAll(findOptions.getSql());
    }

}
