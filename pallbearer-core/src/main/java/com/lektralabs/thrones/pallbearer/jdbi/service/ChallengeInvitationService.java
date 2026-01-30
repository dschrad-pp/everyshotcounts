package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.ChallengeInvitationDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.ChallengeInvitationBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class ChallengeInvitationService extends ChallengeInvitationBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeInvitationService.class);

    private ChallengeInvitationDao challengeInvitationDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.challengeInvitationDao = jdbiProvider.getJdbi().onDemand(ChallengeInvitationDao.class);
    }


}
