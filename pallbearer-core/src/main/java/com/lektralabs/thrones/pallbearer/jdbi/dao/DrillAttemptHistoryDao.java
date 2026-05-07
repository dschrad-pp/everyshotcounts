package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptHistoryRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(DrillAttemptHistoryRow.class)
public interface DrillAttemptHistoryDao {

    @UseStringTemplateSqlLocator
    @SqlUpdate("insertHistory")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean DrillAttemptHistoryRow historyRow);

    @UseStringTemplateSqlLocator
    @SqlQuery("findByDrillId")
    List<DrillAttemptHistoryRow> findByDrillId(@Bind("drillId") UUID drillId);

    @UseStringTemplateSqlLocator
    @SqlQuery("findByUserId")
    List<DrillAttemptHistoryRow> findByUserId(@Bind("userId") UUID userId);

    @UseStringTemplateSqlLocator
    @SqlQuery("findByDrillIdAndUserId")
    List<DrillAttemptHistoryRow> findByDrillIdAndUserId(@Bind("drillId") UUID drillId, @Bind("userId") UUID userId);

    @UseStringTemplateSqlLocator
    @SqlQuery("findById")
    Optional<DrillAttemptHistoryRow> findById(@Bind("id") UUID id);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMediaId")
    int updateMediaId(@Bind("drillId") UUID drillId, @Bind("mediaId") UUID mediaId, @Bind("version") int version);

    @UseStringTemplateSqlLocator
    @SqlQuery("getAttemptCount")
    int getAttemptCount(@Bind("userId") UUID userId, @Bind("drillId") UUID drillId);
}
