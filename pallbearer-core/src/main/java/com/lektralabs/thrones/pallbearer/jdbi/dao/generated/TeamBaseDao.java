package com.lektralabs.thrones.pallbearer.jdbi.dao.generated;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TeamRow;

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

import org.jdbi.v3.sqlobject.customizer.Bind;

// This is generated code. Do not modify - it will be overwritten. See the non-base version.
public interface TeamBaseDao {

    @RegisterBeanMapper(TeamRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<TeamRow> findById(UUID id);

    @RegisterBeanMapper(TeamRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectAll")
    List<TeamRow> findAll(@Define("findOptions") String findOptions);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean TeamRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean TeamRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(UUID id);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insertUserTeamMapping")
    int insertUserTeamMapping(@Bind("userId") UUID userId, @Bind("teamId") UUID teamId);

}
