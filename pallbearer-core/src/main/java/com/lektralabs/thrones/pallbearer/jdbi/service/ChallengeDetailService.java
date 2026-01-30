package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.ChallengeDetailDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeDetail;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ChallengeDetailService {

    private static final Logger logger = Logger.getLogger(ChallengeDetailService.class);

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserService userService;

    private ChallengeDetailDao challengeDetailDao;

    @PostConstruct
    public void init() {
        this.challengeDetailDao = jdbiProvider.getJdbi().onDemand(ChallengeDetailDao.class);
    }

    public Optional<ChallengeDetail> findByChallengeId(UUID challengeId) {
        CurrentUser currentUser = userService.getCurrentUser();
        return challengeDetailDao.getByChallengeId(challengeId, currentUser.getId());
    }

    public List<ChallengeDetail> findFeaturedChallenges() {
        CurrentUser currentUser = userService.getCurrentUser();
        return challengeDetailDao.getFeaturedChallenges(currentUser.getId());
    }

    public Optional<ChallengeDetail> findFeaturedAthleteChallenge(UUID athleteUserId) {
        CurrentUser currentUser = userService.getCurrentUser();
        return challengeDetailDao.getFeaturedAthleteChallenge(athleteUserId, currentUser.getId());
    }

    public List<ChallengeDetail> findWithAthlete(UUID athleteUserId) {
        CurrentUser currentUser = userService.getCurrentUser();
        return challengeDetailDao.getAthleteChallengeDetails(athleteUserId, currentUser.getId());
    }

    public List<ChallengeDetail> findWithFan(UUID fanUserId) {
        CurrentUser currentUser = userService.getCurrentUser();
        return challengeDetailDao.getFanChallengeDetails(fanUserId, currentUser.getId());
    }
}
