package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.ChallengeDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.ChallengeBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class ChallengeService extends ChallengeBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeService.class);

    private ChallengeDao challengeDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.challengeDao = jdbiProvider.getJdbi().onDemand(ChallengeDao.class);
    }


}
