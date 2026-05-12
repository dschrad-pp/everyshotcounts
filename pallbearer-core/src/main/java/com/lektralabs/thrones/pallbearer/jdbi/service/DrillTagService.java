package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillItemTagDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.TagDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TagCategoryRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TagRow;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.stream.Collectors;

@ApplicationScoped
public class DrillTagService {

    @Inject
    JdbiProvider jdbiProvider;

    private TagDao tagDao;
    private DrillItemTagDao drillItemTagDao;

    @PostConstruct
    public void init() {
        this.tagDao = jdbiProvider.getJdbi().onDemand(TagDao.class);
        this.drillItemTagDao = jdbiProvider.getJdbi().onDemand(DrillItemTagDao.class);
    }

    public Map<TagCategoryRow, List<TagRow>> getAllTagCategoriesWithTags() {
        List<TagCategoryRow> categories = tagDao.findAllTagCategories();
        List<TagRow> allTags = tagDao.findAllTags();
        Map<UUID, List<TagRow>> byCategory = allTags.stream()
                .collect(Collectors.groupingBy(TagRow::getTagCategoryId));
        Map<TagCategoryRow, List<TagRow>> result = new LinkedHashMap<>();
        for (TagCategoryRow cat : categories) {
            result.put(cat, byCategory.getOrDefault(cat.getId(), Collections.emptyList()));
        }
        return result;
    }

    public List<TagRow> getTagsForDrillItem(UUID drillItemId) {
        return tagDao.findByDrillItemId(drillItemId);
    }

    public void setTagsForDrillItem(UUID drillItemId, List<UUID> tagIds) {
        drillItemTagDao.deleteAllForDrillItem(drillItemId);
        for (UUID tagId : tagIds) {
            drillItemTagDao.insertTag(drillItemId, tagId);
        }
    }

    public void removeTagFromDrillItem(UUID drillItemId, UUID tagId) {
        drillItemTagDao.deleteTag(drillItemId, tagId);
    }

    public Map<UUID, List<TagRow>> getTagsForDrillItems(List<UUID> drillItemIds) {
        if (drillItemIds == null || drillItemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return tagDao.findByDrillItemIds(drillItemIds).stream()
                .collect(Collectors.groupingBy(TagRow::getDrillItemId));
    }
}
