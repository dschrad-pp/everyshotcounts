package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.common.CoreConstants;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.AthleteDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.UserPropertyDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AthleteService implements CoreConstants {
    private static final Logger logger = Logger.getLogger(AthleteService.class);

    @Inject
    JdbiProvider jdbiProvider;

    private AthleteDao athleteDao;

    private UserPropertyDao userPropertyDao;

    @PostConstruct
    public void init() {
        this.athleteDao = jdbiProvider.getJdbi().onDemand(AthleteDao.class);
        this.userPropertyDao = jdbiProvider.getJdbi().onDemand(UserPropertyDao.class);
    }

    public Optional<AthleteDetail> findByAthleteId(UUID athleteId) {
        Optional<AthleteDetail> maybeAthleteDetail = athleteDao.findByAthleteId(athleteId);
        if ( maybeAthleteDetail.isPresent() ) {
            AthleteDetail athleteDetail = maybeAthleteDetail.get();
            List<UserPropertyRow> userPropertyRows = userPropertyDao.findByUserId(athleteDetail.getUserId());
            Map<String, String> propertyMap = userPropertyRows.stream().collect(
                    Collectors.toMap(UserPropertyRow::getPropertyKey, UserPropertyRow::getPropertyValue));
            athleteDetail.setUserProperties(propertyMap);
            return Optional.of(athleteDetail);
        } else {
            return maybeAthleteDetail;
        }
    }

    public List<AthleteDetail> findAll(FindOptions findOptions) {
        List<AthleteDetail> athleteDetails = athleteDao.findAll(findOptions.getSql());
        List<UUID> athleteUserIds = athleteDetails.stream().map(AthleteDetail::getUserId).toList();
        List<UserPropertyRow> userPropertyRows = userPropertyDao.findByUserIds(athleteUserIds);
        athleteDetails.forEach(ad -> {
            List<UserPropertyRow> athleteProperties =  userPropertyRows.stream().filter(up -> up.getUserId().equals(ad.getUserId())).toList();
            Map<String, String> propertyMap = athleteProperties.stream().collect(
                    Collectors.toMap(UserPropertyRow::getPropertyKey, UserPropertyRow::getPropertyValue));
            ad.setUserProperties(propertyMap);
        });
        return athleteDetails;
    }

    public List<AthleteDetail> findFeaturedAthletes(UUID currentUserId) {
        List<AthleteDetail> athleteDetails = athleteDao.findFeaturedAthletes(currentUserId);
        List<UUID> athleteUserIds = athleteDetails.stream().map(AthleteDetail::getUserId).toList();
        List<UserPropertyRow> userPropertyRows = userPropertyDao.findByUserIds(athleteUserIds);
        athleteDetails.forEach(ad -> {
            List<UserPropertyRow> athleteProperties =  userPropertyRows.stream().filter(up -> up.getUserId().equals(ad.getUserId())).toList();
            Map<String, String> propertyMap = athleteProperties.stream().collect(
                    Collectors.toMap(UserPropertyRow::getPropertyKey, UserPropertyRow::getPropertyValue));
            ad.setUserProperties(propertyMap);
        });
        return athleteDetails;
    }
}

