package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.LeagueAppsRegistrationDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsRegistrationRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.LeagueAppsRegistrationBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Optional;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class LeagueAppsRegistrationService extends LeagueAppsRegistrationBaseService {
    private static final Logger logger = Logger.getLogger(LeagueAppsRegistrationService.class);

    private LeagueAppsRegistrationDao leagueAppsRegistrationDao;

    @PostConstruct
    public void init() {
        super.init();
        this.leagueAppsRegistrationDao = jdbiProvider.getJdbi().onDemand(LeagueAppsRegistrationDao.class);
    }

    public Optional<LeagueAppsRegistrationRow> findByRegistrationId(long registrationId) {
        return leagueAppsRegistrationDao.findByRegistrationId(registrationId);
    }

}
