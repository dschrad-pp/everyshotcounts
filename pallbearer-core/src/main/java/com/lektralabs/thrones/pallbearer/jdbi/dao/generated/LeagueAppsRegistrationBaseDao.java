package com.lektralabs.thrones.pallbearer.jdbi.dao.generated;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsRegistrationRow;
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

// This is generated code. Do not modify - it will be overwritten. See the non-base version.

public interface LeagueAppsRegistrationBaseDao {

    @RegisterBeanMapper(LeagueAppsRegistrationRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<LeagueAppsRegistrationRow> findById(long id);

    @RegisterBeanMapper(LeagueAppsRegistrationRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectAll")
    List<LeagueAppsRegistrationRow> findAll(@Define("findOptions") String findOptions);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    long insert(@BindBean LeagueAppsRegistrationRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean LeagueAppsRegistrationRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(long id);

}
