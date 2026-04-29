package com.lektralabs.thrones.pallbearer.api.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.request.TimeLimitByLevelUpdateRequest;
import com.lektralabs.thrones.pallbearer.api.model.response.TimeLimitByLevelUpdateResponse;
import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillItemDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class DrillItemTimeLimitService {

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserService userService;

    private DrillItemDao drillItemDao;

    private static final Map<String, UUID> LEVEL_TO_GROUP_ID = Map.of(
            "Beginner", DrillGroupConstants.BEGINNER_GROUP_ID,
            "Intermediate", DrillGroupConstants.INTERMEDIATE_GROUP_ID,
            "Advanced", DrillGroupConstants.ADVANCE_GROUP_ID,
            "Elite", DrillGroupConstants.ELITE_GROUP_ID);

    @PostConstruct
    public void init() {
        this.drillItemDao = jdbiProvider.getJdbi().onDemand(DrillItemDao.class);
    }

    @Transactional
    public TimeLimitByLevelUpdateResponse updateTimeLimitByLevel(TimeLimitByLevelUpdateRequest request) {
        String normalizedLevel = normalizeLevelName(request.getLevel());
        UUID drillGroupId = LEVEL_TO_GROUP_ID.get(normalizedLevel);
        if (drillGroupId == null) {
            throw new IllegalArgumentException("Invalid level. Valid levels: Beginner, Intermediate, Advanced, Elite");
        }

        if (request.getTimeLimitMs() == null || request.getTimeLimitMs() <= 0) {
            throw new IllegalArgumentException("timeLimitMs must be greater than 0");
        }

        boolean dryRun = Boolean.TRUE.equals(request.getDryRun());
        Set<Integer> levelIndexSet = request.getLevelIndexes() == null ? Set.of()
                : new LinkedHashSet<>(request.getLevelIndexes());

        List<DrillItemRow> allInGroup = drillItemDao.findByDrillGroupId(drillGroupId);
        List<DrillItemRow> matching = allInGroup.stream()
                .filter(row -> levelIndexSet.isEmpty() || levelIndexSet.contains(row.getLevelIndex()))
                .collect(Collectors.toList());

        if (matching.isEmpty()) {
            return TimeLimitByLevelUpdateResponse.builder()
                    .level(normalizedLevel)
                    .levelIndexes(levelIndexSet.isEmpty() ? List.of() : new ArrayList<>(levelIndexSet))
                    .timeLimitMs(request.getTimeLimitMs())
                    .dryRun(dryRun)
                    .matchedCount(0)
                    .updatedCount(0)
                    .matchedDrills(List.of())
                    .build();
        }

        CurrentUser currentUser = userService.getCurrentUser();
        UUID modifiedById = currentUser.getId();
        long modificationDate = System.currentTimeMillis();

        List<TimeLimitByLevelUpdateResponse.DrillItemTimeLimitChange> changes = new ArrayList<>();
        int updatedCount = 0;
        for (DrillItemRow drill : matching) {
            Long oldTimeLimitMs = drill.getTimeLimitMs();
            if (!dryRun) {
                int rows = drillItemDao.updateTimeLimitMs(
                        drill.getId(),
                        request.getTimeLimitMs(),
                        modificationDate,
                        modifiedById);
                if (rows > 0) {
                    updatedCount++;
                }
            }

            changes.add(TimeLimitByLevelUpdateResponse.DrillItemTimeLimitChange.builder()
                    .drillItemId(drill.getId())
                    .name(drill.getName().orElse(""))
                    .levelIndex(drill.getLevelIndex())
                    .oldTimeLimitMs(oldTimeLimitMs)
                    .newTimeLimitMs(request.getTimeLimitMs())
                    .build());
        }

        return TimeLimitByLevelUpdateResponse.builder()
                .level(normalizedLevel)
                .levelIndexes(levelIndexSet.isEmpty() ? List.of() : new ArrayList<>(levelIndexSet))
                .timeLimitMs(request.getTimeLimitMs())
                .dryRun(dryRun)
                .matchedCount(matching.size())
                .updatedCount(dryRun ? 0 : updatedCount)
                .matchedDrills(changes)
                .build();
    }

    private String normalizeLevelName(String level) {
        if (level == null || level.trim().isEmpty()) {
            return null;
        }
        String normalized = level.trim();
        normalized = normalized.substring(0, 1).toUpperCase()
                + (normalized.length() > 1 ? normalized.substring(1).toLowerCase() : "");
        if ("Expert".equalsIgnoreCase(normalized) || "Advance".equalsIgnoreCase(normalized)) {
            return "Advanced";
        }
        return normalized;
    }
}
