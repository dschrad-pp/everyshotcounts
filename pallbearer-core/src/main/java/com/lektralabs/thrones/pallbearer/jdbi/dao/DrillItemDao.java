package com.lektralabs.thrones.pallbearer.jdbi.dao;

import java.util.List;
import java.util.UUID;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper; // Import this
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import com.lektralabs.thrones.pallbearer.datetime.jdbi.DrillItemDetailMapper;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;

public interface DrillItemDao {

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMediaId")
    int updateMediaId(UUID drillItemId, UUID mediaId);

    @UseStringTemplateSqlLocator
    @SqlQuery("findMaxItemOrder")
    int findMaxItemOrder(UUID drillGroupId, int drillLevelIndex);

    @UseStringTemplateSqlLocator
    @RegisterRowMapper(DrillItemDetailMapper.class) // Register your custom mapper for DrillItemDetail
    @SqlQuery("findAll")
    List<DrillItemDetail> findAll();

    @UseStringTemplateSqlLocator
    @RegisterBeanMapper(DrillItemRow.class)
    @SqlQuery("findByDrillGroupId")
    List<DrillItemRow> findByDrillGroupId(
            @org.jdbi.v3.sqlobject.customizer.Bind("drillGroupId") UUID drillGroupId);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updatePassingScore")
    int updatePassingScore(
            @org.jdbi.v3.sqlobject.customizer.Bind("drillItemId") UUID drillItemId,
            @org.jdbi.v3.sqlobject.customizer.Bind("passingScore") Integer passingScore,
            @org.jdbi.v3.sqlobject.customizer.Bind("modificationDate") Long modificationDate,
            @org.jdbi.v3.sqlobject.customizer.Bind("modifiedById") UUID modifiedById);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateTimeLimitMs")
    int updateTimeLimitMs(
            @org.jdbi.v3.sqlobject.customizer.Bind("drillItemId") UUID drillItemId,
            @org.jdbi.v3.sqlobject.customizer.Bind("timeLimitMs") Long timeLimitMs,
            @org.jdbi.v3.sqlobject.customizer.Bind("modificationDate") Long modificationDate,
            @org.jdbi.v3.sqlobject.customizer.Bind("modifiedById") UUID modifiedById);

    @UseStringTemplateSqlLocator
    @RegisterBeanMapper(DrillItemThumbnailRow.class)
    @SqlQuery("findAllWithGoogleDriveThumbnails")
    List<DrillItemThumbnailRow> findAllWithGoogleDriveThumbnails();

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMediaThumbnail")
    int updateMediaThumbnail(
            @org.jdbi.v3.sqlobject.customizer.Bind("drillItemId") UUID drillItemId,
            @org.jdbi.v3.sqlobject.customizer.Bind("mediaThumbnail") String mediaThumbnail,
            @org.jdbi.v3.sqlobject.customizer.Bind("modificationDate") Long modificationDate);

    @UseStringTemplateSqlLocator
    @RegisterBeanMapper(com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName.class)
    @SqlQuery("findAllWithGroupNames")
    List<com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> findAllWithGroupNames();

    @UseStringTemplateSqlLocator
    @RegisterBeanMapper(com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName.class)
    @SqlQuery("findByMediaId")
    List<com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> findByMediaId(
            @org.jdbi.v3.sqlobject.customizer.Bind("mediaId") UUID mediaId);

    @UseStringTemplateSqlLocator
    @RegisterBeanMapper(com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName.class)
    @SqlQuery("findByUniqueId")
    java.util.Optional<com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName> findByUniqueId(
            @org.jdbi.v3.sqlobject.customizer.Bind("uniqueId") String uniqueId);

    /**
     * Simple row class for drill item thumbnail data
     */
    class DrillItemThumbnailRow {
        private UUID id;
        private String mediaThumbnail;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getMediaThumbnail() {
            return mediaThumbnail;
        }

        public void setMediaThumbnail(String mediaThumbnail) {
            this.mediaThumbnail = mediaThumbnail;
        }
    }
}
