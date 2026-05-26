package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.AthleteSnapshotStatsRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.DrillSkillTagRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.DrillStatsRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.SkillBreakdownRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.UUID;

@UseStringTemplateSqlLocator
public interface CoachAthleteSnapshotDao {

    @RegisterBeanMapper(AthleteSnapshotStatsRow.class)
    @SqlQuery("getAthleteStats")
    AthleteSnapshotStatsRow getAthleteStats(@Bind("athleteId") UUID athleteId);

    @RegisterBeanMapper(SkillBreakdownRow.class)
    @SqlQuery("getSkillBreakdown")
    List<SkillBreakdownRow> getSkillBreakdown(@Bind("athleteId") UUID athleteId);

    @RegisterBeanMapper(DrillStatsRow.class)
    @SqlQuery("getDrillStats")
    List<DrillStatsRow> getDrillStats(
            @Bind("athleteId") UUID athleteId,
            @Bind("limit") int limit,
            @Bind("offset") int offset);

    @RegisterBeanMapper(DrillStatsRow.class)
    @SqlQuery("getDrillStatsWithTagFilter")
    List<DrillStatsRow> getDrillStatsWithTagFilter(
            @Bind("athleteId") UUID athleteId,
            @BindList(value = "tagCodes", onEmpty = BindList.EmptyHandling.NULL) List<String> tagCodes,
            @Bind("limit") int limit,
            @Bind("offset") int offset);

    @RegisterBeanMapper(DrillSkillTagRow.class)
    @SqlQuery("getSkillTagsForDrillItems")
    List<DrillSkillTagRow> getSkillTagsForDrillItems(
            @BindList(value = "drillItemIds", onEmpty = BindList.EmptyHandling.NULL) List<UUID> drillItemIds);
}
