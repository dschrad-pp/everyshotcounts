package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.ChallengeEntryDetailDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeEntryDetail;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ChallengeEntryDetailService {

    private static final Logger logger = Logger.getLogger(ChallengeEntryDetailService.class);

    @Inject
    JdbiProvider jdbiProvider;

    private ChallengeEntryDetailDao challengeEntryDetailDao;

    @PostConstruct
    public void init() {
        this.challengeEntryDetailDao = jdbiProvider.getJdbi().onDemand(ChallengeEntryDetailDao.class);
    }

    public Optional<ChallengeEntryDetail> findByChallengeEntryId(UUID challengeEntryId) {
        return challengeEntryDetailDao.getByChallengeEntryId(challengeEntryId);
    }

    public List<ChallengeEntryDetail> findByChallengeId(UUID challengeId) {
        return challengeEntryDetailDao.getByChallengeId(challengeId);
    }
}
