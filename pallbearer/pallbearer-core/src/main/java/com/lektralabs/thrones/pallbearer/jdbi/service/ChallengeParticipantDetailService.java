package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.ChallengeParticipantDetailDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeParticipantDetail;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ChallengeParticipantDetailService {

    private static final Logger logger = Logger.getLogger(ChallengeParticipantDetailService.class);

    @Inject
    JdbiProvider jdbiProvider;

    private ChallengeParticipantDetailDao challengeParticipantDetailDao;

    @PostConstruct
    public void init() {
        this.challengeParticipantDetailDao = jdbiProvider.getJdbi().onDemand(ChallengeParticipantDetailDao.class);
    }

    public List<ChallengeParticipantDetail> selectByChallengeId(UUID challengeId) {
        return challengeParticipantDetailDao.selectByChallengeId(challengeId);
    }
}
