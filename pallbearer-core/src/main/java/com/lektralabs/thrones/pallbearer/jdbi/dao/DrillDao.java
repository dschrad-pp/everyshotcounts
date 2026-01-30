package com.lektralabs.thrones.pallbearer.jdbi.dao;

import java.util.List;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.sqlobject.customizer.Bind;

public interface DrillDao {

    @RegisterBeanMapper(DrillRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByDrillItemIdAndUserId")
    Optional<DrillRow> findByDrillItemIdAndUserId(UUID drillItemId, UUID userId);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMediaId")
    int updateMediaId(UUID drillId, UUID mediaId);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateAttemptsAndMakesDetected")
    int updateAttemptsAndMakesDetected(UUID drillId, int attemptsDetected, int makesDetected);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateDrillStatus")
    int updateDrillStatus(UUID drillId, String drillStatusCode);

    @RegisterBeanMapper(DrillRow.class)
    @SqlQuery("SELECT id, drill_item_id, user_id, media_id, drill_status, creation_date, modification_date, created_by_id, modified_by_id, version, attempts_detected, attempts_reported, makes_detected, makes_reported "
            + "FROM t_drill "
            + "WHERE user_id = :userId "
            + "ORDER BY modification_date DESC "
            + "LIMIT :limit")
    List<DrillRow> findRecentDrillsByUserId(@Bind("userId") UUID userId, @Bind("limit") int limit);

    @RegisterBeanMapper(DrillRow.class)
    @SqlQuery("SELECT id, drill_item_id, user_id, media_id, drill_status, creation_date, modification_date, created_by_id, modified_by_id, version, attempts_detected, attempts_reported, makes_detected, makes_reported "
            + "FROM t_drill "
            + "WHERE user_id = :userId "
            + "ORDER BY modification_date DESC")
    List<DrillRow> findAllDrillsByUserId(@Bind("userId") UUID userId);
}
