package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.TeamPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.TeamBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TeamRow;
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
public class TeamBaseService {

    private static final Logger logger = Logger.getLogger(TeamBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private TeamBaseDao teamBaseDao;

    @PostConstruct
    public void init() {
        this.teamBaseDao = jdbiProvider.getJdbi().onDemand(TeamBaseDao.class);
    }

    @Transactional
    public UUID create(TeamPartial teamPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                TeamRow teamRow = teamPartial.toRow(auditUser);
                return teamBaseDao.insert(teamRow);
            } catch (Exception e) {
                logger.warn("Error creating a [team]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(TeamPartial teamPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                TeamRow teamRow = teamPartial.toRow(auditUser);
                return teamBaseDao.update(teamRow);
            } catch (Exception e) {
                logger.warn("Error updating a [team]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<TeamRow> findById(UUID id) {
        return teamBaseDao.findById(id);
    }

    public List<TeamRow> findAll(FindOptions findOptions) {
        return teamBaseDao.findAll(findOptions.getSql());
    }

    // @Transactional // Keep this @Transactional
    public void mapUserToTeam(UUID userId, UUID teamId) {
        try {
            teamBaseDao.insertUserTeamMapping(userId, teamId);
        } catch (Exception e) {
            logger.warnf(e, "Failed to map user %s to team %s", userId, teamId);
            throw new RuntimeException("Failed to map user to team", e);
        }
    }

}
