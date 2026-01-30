package com.lektralabs.thrones.pallbearer.api.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.request.PassingScoreUpdateRequest;
import com.lektralabs.thrones.pallbearer.api.model.response.PassingScoreUpdateResponse;
import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillItemDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class DrillItemPassingScoreService {

    private static final Logger logger = Logger.getLogger(DrillItemPassingScoreService.class);

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserService userService;

    private DrillItemDao drillItemDao;

    // Map level names to drill group IDs
    private static final Map<String, UUID> LEVEL_TO_GROUP_ID = Map.of(
            "Beginner", DrillGroupConstants.BEGINNER_GROUP_ID,
            "Intermediate", DrillGroupConstants.INTERMEDIATE_GROUP_ID,
            // "Expert", DrillGroupConstants.ADVANCE_GROUP_ID,
            "Advanced", DrillGroupConstants.ADVANCE_GROUP_ID,
            "Elite", DrillGroupConstants.ELITE_GROUP_ID
    );

    @PostConstruct
    public void init() {
        this.drillItemDao = jdbiProvider.getJdbi().onDemand(DrillItemDao.class);
    }

    @Transactional
    public PassingScoreUpdateResponse updatePassingScores(PassingScoreUpdateRequest request) {
        List<PassingScoreUpdateResponse.UpdatedDrill> updatedDrills = new ArrayList<>();
        List<PassingScoreUpdateResponse.NotFoundDrill> notFound = new ArrayList<>();

        PassingScoreUpdateRequest.MatchOptions options = request.getOptions();
        if (options == null) {
            options = PassingScoreUpdateRequest.MatchOptions.builder()
                    .matchByName(true)
                    .matchByDescription(true)
                    .caseSensitive(false)
                    .allowPartialMatch(false)
                    .build();
        }

        CurrentUser currentUser = userService.getCurrentUser();
        UUID modifiedById = currentUser.getId();
        Long modificationDate = System.currentTimeMillis();

        for (PassingScoreUpdateRequest.DrillUpdate update : request.getUpdates()) {
            try {
                // Normalize level name
                String normalizedLevel = normalizeLevelName(update.getLevel());
                UUID drillGroupId = LEVEL_TO_GROUP_ID.get(normalizedLevel);

                if (drillGroupId == null) {
                    notFound.add(PassingScoreUpdateResponse.NotFoundDrill.builder()
                            .level(update.getLevel())
                            .name(update.getName())
                            .description(update.getDescription())
                            .reason("Invalid level name: " + update.getLevel())
                            .build());
                    continue;
                }

                // Find matching drills
                List<DrillItemRow> matchingDrills = findMatchingDrills(
                        drillGroupId,
                        update.getName(),
                        update.getDescription(),
                        update.getLevelIndex(),
                        options
                );

                if (matchingDrills.isEmpty()) {
                    notFound.add(PassingScoreUpdateResponse.NotFoundDrill.builder()
                            .level(normalizedLevel)
                            .name(update.getName())
                            .description(update.getDescription())
                            .reason("No matching drill found with the specified criteria")
                            .build());
                    continue;
                }

                // Update each matching drill
                for (DrillItemRow drill : matchingDrills) {
                    Integer oldPassingScore = drill.getPassingScore();
                    int updateCount = drillItemDao.updatePassingScore(
                            drill.getId(),
                            update.getPassingScore(),
                            modificationDate,
                            modifiedById
                    );

                    if (updateCount > 0) {
                        updatedDrills.add(PassingScoreUpdateResponse.UpdatedDrill.builder()
                                .id(drill.getId())
                                .name(drill.getName().orElse(""))
                                .description(drill.getDescription().orElse(""))
                                .oldPassingScore(oldPassingScore)
                                .newPassingScore(update.getPassingScore())
                                .level(normalizedLevel)
                                .build());
                        logger.info(String.format("Updated passing score for drill: id=%s, name=%s, oldScore=%d, newScore=%d",
                                drill.getId(), drill.getName().orElse(""), oldPassingScore, update.getPassingScore()));
                    }
                }

            } catch (Exception e) {
                logger.error(String.format("Error updating passing score for drill: level=%s, name=%s", 
                        update.getLevel(), update.getName()), e);
                notFound.add(PassingScoreUpdateResponse.NotFoundDrill.builder()
                        .level(update.getLevel())
                        .name(update.getName())
                        .description(update.getDescription())
                        .reason("Error: " + e.getMessage())
                        .build());
            }
        }

        return PassingScoreUpdateResponse.builder()
                .totalRequested(request.getUpdates().size())
                .totalUpdated(updatedDrills.size())
                .totalNotFound(notFound.size())
                .updatedDrills(updatedDrills)
                .notFound(notFound)
                .build();
    }

    private List<DrillItemRow> findMatchingDrills(
            UUID drillGroupId,
            String name,
            String description,
            Integer levelIndex,
            PassingScoreUpdateRequest.MatchOptions options) {

        // Get all drills for the group first
        List<DrillItemRow> allDrills = drillItemDao.findByDrillGroupId(drillGroupId);

        // Filter based on match options
        return allDrills.stream()
                .filter(drill -> matchesDrill(drill, name, description, levelIndex, options))
                .collect(Collectors.toList());
    }

    private boolean matchesDrill(
            DrillItemRow drill,
            String name,
            String description,
            Integer levelIndex,
            PassingScoreUpdateRequest.MatchOptions options) {

        boolean nameMatches = true;
        boolean descriptionMatches = true;
        boolean levelIndexMatches = true;

        // Match by name
        if (Boolean.TRUE.equals(options.getMatchByName()) && name != null && !name.trim().isEmpty()) {
            String drillName = drill.getName().orElse("");
            String searchName = name.trim();

            if (Boolean.TRUE.equals(options.getCaseSensitive())) {
                if (Boolean.TRUE.equals(options.getAllowPartialMatch())) {
                    nameMatches = drillName.contains(searchName);
                } else {
                    nameMatches = drillName.equals(searchName);
                }
            } else {
                if (Boolean.TRUE.equals(options.getAllowPartialMatch())) {
                    nameMatches = drillName.toLowerCase().contains(searchName.toLowerCase());
                } else {
                    nameMatches = drillName.equalsIgnoreCase(searchName);
                }
            }
        }

        // Match by description
        if (Boolean.TRUE.equals(options.getMatchByDescription()) && description != null && !description.trim().isEmpty()) {
            String drillDescription = drill.getDescription().orElse("");
            String searchDescription = description.trim();

            if (Boolean.TRUE.equals(options.getCaseSensitive())) {
                if (Boolean.TRUE.equals(options.getAllowPartialMatch())) {
                    descriptionMatches = drillDescription.contains(searchDescription);
                } else {
                    descriptionMatches = drillDescription.equals(searchDescription);
                }
            } else {
                if (Boolean.TRUE.equals(options.getAllowPartialMatch())) {
                    descriptionMatches = drillDescription.toLowerCase().contains(searchDescription.toLowerCase());
                } else {
                    descriptionMatches = drillDescription.equalsIgnoreCase(searchDescription);
                }
            }
        }

        // Match by level index (if provided)
        if (levelIndex != null) {
            levelIndexMatches = Objects.equals(drill.getLevelIndex(), levelIndex);
        }

        return nameMatches && descriptionMatches && levelIndexMatches;
    }

    private String normalizeLevelName(String level) {
        if (level == null || level.trim().isEmpty()) {
            return null;
        }

        String normalized = level.trim();
        if (normalized.length() > 0) {
            normalized = normalized.substring(0, 1).toUpperCase() +
                    (normalized.length() > 1 ? normalized.substring(1).toLowerCase() : "");
        }

        // Handle "Expert" and "Advance" as the same
        if ("Expert".equalsIgnoreCase(normalized)) {
            normalized = "Advance";
        }

        return normalized;
    }
}

