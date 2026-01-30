package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsMemberActionPartial;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.LeagueAppsMemberActionDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsMemberActionRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.LeagueAppsMemberActionBaseService;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class LeagueAppsMemberActionService extends LeagueAppsMemberActionBaseService {
    private static final Logger logger = Logger.getLogger(LeagueAppsMemberActionService.class);

    private LeagueAppsMemberActionDao leagueAppsMemberActionDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.leagueAppsMemberActionDao = jdbiProvider.getJdbi().onDemand(LeagueAppsMemberActionDao.class);
    }


}
