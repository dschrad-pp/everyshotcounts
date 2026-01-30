package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.TeamDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.TeamBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class TeamService extends TeamBaseService {
    private static final Logger logger = Logger.getLogger(TeamService.class);

    private TeamDao teamDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.teamDao = jdbiProvider.getJdbi().onDemand(TeamDao.class);
    }


}
