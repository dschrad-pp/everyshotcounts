package com.lektralabs.thrones.pallbearer.jdbi.dao.generated;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SportChallengeTypeRow;
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

public interface SportChallengeTypeBaseDao {

    @RegisterBeanMapper(SportChallengeTypeRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<SportChallengeTypeRow> findById(UUID id);

    @RegisterBeanMapper(SportChallengeTypeRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectAll")
    List<SportChallengeTypeRow> findAll(@Define("findOptions") String findOptions);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean SportChallengeTypeRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean SportChallengeTypeRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(UUID id);

}
