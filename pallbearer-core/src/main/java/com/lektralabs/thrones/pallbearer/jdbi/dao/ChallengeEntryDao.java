package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeEntryRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeEntryDao {

    @RegisterBeanMapper(ChallengeEntryRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<ChallengeEntryRow> findById(UUID id);

    @RegisterBeanMapper(ChallengeEntryRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByChallenge")
    List<ChallengeEntryRow> findByChallenge(UUID challengeId);

    @RegisterBeanMapper(ChallengeEntryRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByChallengeAndUser")
    Optional<ChallengeEntryRow> findByChallengeAndUser(UUID challengeId,
                                                       UUID userId);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean ChallengeEntryRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean ChallengeEntryRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(UUID id);

}
