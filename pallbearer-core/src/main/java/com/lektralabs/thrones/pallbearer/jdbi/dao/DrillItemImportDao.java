package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.DrillItemWithGroupName;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemImportRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrillItemImportDao {

    @RegisterBeanMapper(DrillItemImportRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<DrillItemImportRow> findById(UUID id);

    @RegisterBeanMapper(DrillItemImportRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectAll")
    List<DrillItemImportRow> findAll(@Define("findOptions") String findOptions);

    @RegisterBeanMapper(DrillItemImportRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("findByUniqueId")
    Optional<DrillItemImportRow> findByUniqueId(String uniqueId);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean DrillItemImportRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean DrillItemImportRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(UUID id);

    @UseStringTemplateSqlLocator
    @SqlUpdate("deleteAll")
    int deleteAll();

    @UseStringTemplateSqlLocator
    @SqlUpdate("deleteAllFromProduction")
    int deleteAllFromProduction();

    @UseStringTemplateSqlLocator
    @SqlUpdate("migrateToProduction")
    int migrateToProduction();

    @RegisterBeanMapper(DrillItemWithGroupName.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("findAllWithGroupNames")
    List<DrillItemWithGroupName> findAllWithGroupNames();

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMediaThumbnail")
    int updateMediaThumbnail(UUID id, String mediaThumbnail, Long modificationDate);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMediaThumbnailPathPrefix")
    int updateMediaThumbnailPathPrefix(String oldPathPrefix, String newPathPrefix, Long modificationDate);

}

