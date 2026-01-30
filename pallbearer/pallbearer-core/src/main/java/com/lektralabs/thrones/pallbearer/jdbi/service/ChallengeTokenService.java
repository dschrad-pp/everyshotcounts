package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.ChallengeTokenDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.ChallengeTokenBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class ChallengeTokenService extends ChallengeTokenBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeTokenService.class);

    private ChallengeTokenDao challengeTokenDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.challengeTokenDao = jdbiProvider.getJdbi().onDemand(ChallengeTokenDao.class);
    }


}
