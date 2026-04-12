package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.AthleteDrillDetailDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.lektralabs.thrones.pallbearer.jdbi.dao.CoachDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.CoachPartial;
import com.lektralabs.thrones.pallbearer.jdbi.dao.UserPropertyDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserGroupPropertyRow;

import com.lektralabs.thrones.pallbearer.jdbi.dao.UserGroupPropertyDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import com.lektralabs.thrones.pallbearer.util.PropertyKeyUtils;

@ApplicationScoped
public class CoachDrillService {

    private static Logger logger = LoggerFactory.getLogger(CoachDrillService.class);

    @Inject
    JdbiProvider jdbiProvider;

    CoachDao coachDao;
    UserPropertyDao userPropertyDao;
    UserGroupPropertyDao userGroupPropertyDao;
    private AthleteDrillDetailDao athleteDrillDetailDao;

    @PostConstruct
    public void init() {
        this.athleteDrillDetailDao = jdbiProvider.getJdbi().onDemand(AthleteDrillDetailDao.class);
        this.coachDao = jdbiProvider.getJdbi().onDemand(CoachDao.class);
        this.userPropertyDao = jdbiProvider.getJdbi().onDemand(UserPropertyDao.class);
        this.userGroupPropertyDao = jdbiProvider.getJdbi().onDemand(UserGroupPropertyDao.class);

    }

    public List<AthleteDrillDetail> findWithTeamTimeline(UUID teamId,
            FindOptions findOptions) {
        List<AthleteDrillDetail> athleteDrillDetails = athleteDrillDetailDao
                .getByTeamTimeline(teamId, findOptions);

        return athleteDrillDetails.stream().map(row -> {
            row.setIsLocked(false);
            return row;
        }).toList();
    }

    public Optional<CoachPartial> findCoachByUsername(String username) {
        return coachDao.findByUsername(username);
    }

    public Optional<CoachPartial> findCoachById(UUID coachId) {
        return coachDao.findById(coachId);
    }

    public List<AthleteDetail> findAllAthletesAssignedToCoach(UUID coachId) {
        List<AthleteDetail> athleteDetails = coachDao.selectAllAthletesAssignedToCoach(coachId);
        List<UUID> athleteUserIds = athleteDetails.stream()
                .map(AthleteDetail::getUserId)
                .distinct() // Get distinct user IDs to avoid redundant queries
                .toList();

        if (athleteUserIds.isEmpty()) {
            return List.of();
        }

        List<UserPropertyRow> userPropertyRows = userPropertyDao.findByUserIds(athleteUserIds);

        Map<UUID, List<UserPropertyRow>> userPropertiesMap = userPropertyRows.stream()
                .collect(Collectors.groupingBy(UserPropertyRow::getUserId));

        List<UserGroupPropertyRow> userGroupProperties = userGroupPropertyDao.findByUserIds(athleteUserIds);

        Map<UUID, List<UserGroupPropertyRow>> userGroupPropertiesMap = userGroupProperties.stream()
                .collect(Collectors.groupingBy(UserGroupPropertyRow::getUserId));

        athleteDetails.forEach(athleteDetail -> {
            List<UserPropertyRow> propertiesForThisAthlete = userPropertiesMap.getOrDefault(athleteDetail.getUserId(),
                    List.of());
            List<UserGroupPropertyRow> groupPropertiesForThisAthlete = userGroupPropertiesMap
                    .getOrDefault(athleteDetail.getUserId(), List.of());

            Map<String, String> propertyMap = propertiesForThisAthlete.stream()
                    .collect(Collectors.toMap(UserPropertyRow::getPropertyKey, UserPropertyRow::getPropertyValue));
            Map<String, String> camelCaseUserProperties = PropertyKeyUtils.convertKeysToCamelCase(propertyMap);

            Map<String, Map<String, String>> groupProperties = new HashMap<>();
            for (UserGroupPropertyRow row : groupPropertiesForThisAthlete) {
                UUID groupId = row.getDrillGroupId();
                String groupName = DrillGroupConstants.drillGroupIdNameMap.getOrDefault(groupId, groupId.toString());
                groupProperties
                        .computeIfAbsent(groupName, k -> new HashMap<>())
                        .put(row.getPropertyKey(), row.getPropertyValue());
            }
            athleteDetail.setGroupProperties(groupProperties);
            athleteDetail.setUserProperties(camelCaseUserProperties);
        });

        return athleteDetails;

    }
}
