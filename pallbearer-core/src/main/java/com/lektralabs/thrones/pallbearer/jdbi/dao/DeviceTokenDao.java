package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.DeviceTokenRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.Optional;
import java.util.UUID;

@UseStringTemplateSqlLocator
public interface DeviceTokenDao {

    @RegisterBeanMapper(DeviceTokenRow.class)
    @SqlQuery("findByUserId")
    Optional<DeviceTokenRow> findByUserId(@Bind("userId") UUID userId, @Bind("platform") String platform);

    @SqlUpdate("upsert")
    void upsert(@BindBean DeviceTokenRow row);

    @SqlUpdate("deleteByUserId")
    void deleteByUserId(@Bind("userId") UUID userId, @Bind("platform") String platform);

    @SqlUpdate("deleteAllByUserId")
    int deleteAllByUserId(@Bind("userId") UUID userId);
}
