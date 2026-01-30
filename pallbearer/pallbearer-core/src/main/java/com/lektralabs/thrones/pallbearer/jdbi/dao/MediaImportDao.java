package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.MediaImportRow;
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

public interface MediaImportDao {

    @RegisterBeanMapper(MediaImportRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<MediaImportRow> findById(UUID id);

    @RegisterBeanMapper(MediaImportRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectAll")
    List<MediaImportRow> findAll(@Define("findOptions") String findOptions);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean MediaImportRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean MediaImportRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(UUID id);

    @UseStringTemplateSqlLocator
    @SqlUpdate("deleteAll")
    int deleteAll();

    @RegisterBeanMapper(MediaImportRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("findByContentUrl")
    Optional<MediaImportRow> findByContentUrl(String contentUrl);

    @UseStringTemplateSqlLocator
    @SqlUpdate("deleteAllFromProduction")
    int deleteAllFromProduction();

    @UseStringTemplateSqlLocator
    @SqlUpdate("migrateToProduction")
    int migrateToProduction();

    @RegisterBeanMapper(MediaImportRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("findAllWithGoogleDriveUrls")
    List<MediaImportRow> findAllWithGoogleDriveUrls();

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateContentUrl")
    int updateContentUrl(UUID id, String contentUrl, Long modificationDate);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateContentUrlPathPrefix")
    int updateContentUrlPathPrefix(String oldPathPrefix, String newPathPrefix, Long modificationDate);

}
