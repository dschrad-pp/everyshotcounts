package com.lektralabs.thrones.pallbearer.jdbi.dao;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.UUID;

public interface MediaDao {

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMediaStatus")
    int updateMediaStatus(UUID mediaId, String mediaStatusCode);

    @UseStringTemplateSqlLocator
    @RegisterBeanMapper(MediaContentUrlRow.class)
    @SqlQuery("findAllWithGoogleDriveContentUrls")
    List<MediaContentUrlRow> findAllWithGoogleDriveContentUrls();

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateContentUrl")
    int updateContentUrl(
            @org.jdbi.v3.sqlobject.customizer.Bind("mediaId") UUID mediaId,
            @org.jdbi.v3.sqlobject.customizer.Bind("contentUrl") String contentUrl,
            @org.jdbi.v3.sqlobject.customizer.Bind("modificationDate") Long modificationDate);

    /**
     * Simple row class for media content URL data
     */
    class MediaContentUrlRow {
        private UUID id;
        private String contentUrl;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getContentUrl() {
            return contentUrl;
        }

        public void setContentUrl(String contentUrl) {
            this.contentUrl = contentUrl;
        }
    }
}
