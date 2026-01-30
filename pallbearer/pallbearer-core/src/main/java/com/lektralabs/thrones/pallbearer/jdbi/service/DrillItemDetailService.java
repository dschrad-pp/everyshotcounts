package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillItemDetailDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DrillItemDetailService {

    private static final Logger logger = LoggerFactory.getLogger(DrillItemDetailService.class);

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserService userService;

    private DrillItemDetailDao drillItemDetailDao;

    @PostConstruct
    public void init() {
        this.drillItemDetailDao = jdbiProvider.getJdbi().onDemand(DrillItemDetailDao.class);
    }

    public Optional<DrillItemDetail> findByDrillItemId(UUID drillItemId) {
        return drillItemDetailDao.getByDrillItemId(drillItemId);
    }

    public List<DrillItemDetail> getTeamDrillItemDetails(UUID teamId) {
        return drillItemDetailDao.getTeamDrillItemDetails(teamId);
    }

    public List<DrillItemDetail> findDrillItemByGroupId(UUID groupId) {
        logger.info("Service: Looking for drill items with group ID: {}", groupId);
        List<DrillItemDetail> results = drillItemDetailDao.findDrillItemByGroupId(groupId);
        logger.info("Service: DAO returned {} results", results.size());
        return results;
    }
}
