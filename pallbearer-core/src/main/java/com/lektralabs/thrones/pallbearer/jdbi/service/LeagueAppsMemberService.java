package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.LeagueAppsMemberDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsMemberRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.LeagueAppsMemberBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class LeagueAppsMemberService extends LeagueAppsMemberBaseService {
    private static final Logger logger = Logger.getLogger(LeagueAppsMemberService.class);

    private LeagueAppsMemberDao leagueAppsMemberDao;

    @PostConstruct
    public void init() {
        super.init();
        this.leagueAppsMemberDao = jdbiProvider.getJdbi().onDemand(LeagueAppsMemberDao.class);
    }

    public Optional<LeagueAppsMemberRow> findByUsername(String username) {
        return leagueAppsMemberDao.findByUsername(username);
    }

}
