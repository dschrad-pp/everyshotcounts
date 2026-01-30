package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.ChallengeParticipantDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.ChallengeParticipantBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class ChallengeParticipantService extends ChallengeParticipantBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeParticipantService.class);

    private ChallengeParticipantDao challengeParticipantDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.challengeParticipantDao = jdbiProvider.getJdbi().onDemand(ChallengeParticipantDao.class);
    }


}
