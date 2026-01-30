package com.lektralabs.thrones.pallbearer.jdbi.service.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.EscLeagueAppsMemberActionPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.EscLeagueAppsMemberActionBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.EscLeagueAppsMemberActionRow;
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
public class EscLeagueAppsMemberActionBaseService {
    private static final Logger logger = Logger.getLogger(EscLeagueAppsMemberActionBaseService.class);

    @Inject
    protected JdbiProvider jdbiProvider;

    @Inject
    protected UserService userService;

    private EscLeagueAppsMemberActionBaseDao escLeagueAppsMemberActionBaseDao;

    @PostConstruct
    public void init() {
        this.escLeagueAppsMemberActionBaseDao = jdbiProvider.getJdbi().onDemand(EscLeagueAppsMemberActionBaseDao.class);
    }

    @Transactional
    public UUID create(EscLeagueAppsMemberActionPartial escLeagueAppsMemberActionPartial) {
        UUID rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                EscLeagueAppsMemberActionRow escLeagueAppsMemberActionRow = escLeagueAppsMemberActionPartial.toRow(auditUser);
                return escLeagueAppsMemberActionBaseDao.insert(escLeagueAppsMemberActionRow);
            } catch (Exception e) {
                logger.warn("Error creating a [escLeagueAppsMemberAction]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    @Transactional
    public int update(EscLeagueAppsMemberActionPartial escLeagueAppsMemberActionPartial) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                CurrentUser auditUser = userService.getCurrentUser();
                EscLeagueAppsMemberActionRow escLeagueAppsMemberActionRow = escLeagueAppsMemberActionPartial.toRow(auditUser);
                return escLeagueAppsMemberActionBaseDao.update(escLeagueAppsMemberActionRow);
            } catch (Exception e) {
                logger.warn("Error updating a [escLeagueAppsMemberAction]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<EscLeagueAppsMemberActionRow> findById(UUID id) {
        return escLeagueAppsMemberActionBaseDao.findById(id);
    }

    public List<EscLeagueAppsMemberActionRow> findAll(FindOptions findOptions) {
        return escLeagueAppsMemberActionBaseDao.findAll(findOptions.getSql());
    }

}
