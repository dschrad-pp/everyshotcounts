package com.lektralabs.thrones.pallbearer.jdbi.dao;

// This is generated code. You are free to modify - it will not be overwritten

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TeamRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.Optional;
import java.util.UUID;

@UseStringTemplateSqlLocator
public interface TeamDao {

    @SqlUpdate("insertJoinCode")
    void insertJoinCode(@Bind("teamId") UUID teamId, @Bind("joinCode") String joinCode);

    @RegisterBeanMapper(TeamRow.class)
    @SqlQuery("findTeamByJoinCode")
    Optional<TeamRow> findTeamByJoinCode(@Bind("joinCode") String joinCode);

    @SqlQuery("findJoinCodeByTeamId")
    Optional<String> findJoinCodeByTeamId(@Bind("teamId") UUID teamId);

    @SqlQuery("userHasTeam")
    boolean userHasTeam(@Bind("userId") UUID userId);

}
