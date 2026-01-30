package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.ChallengeEntryVoteDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.ChallengeEntryVoteBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class ChallengeEntryVoteService extends ChallengeEntryVoteBaseService {
    private static final Logger logger = Logger.getLogger(ChallengeEntryVoteService.class);

    private ChallengeEntryVoteDao challengeEntryVoteDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.challengeEntryVoteDao = jdbiProvider.getJdbi().onDemand(ChallengeEntryVoteDao.class);
    }


}
