package com.lektralabs.thrones.pallbearer.jdbi.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.AllowUnusedBindings;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.CoachPartial;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.reducer.AthleteDetailRowReducer;

@UseStringTemplateSqlLocator
public interface CoachDao {

    @RegisterBeanMapper(CoachPartial.class)
    @SqlQuery("selectByUsername")
    Optional<CoachPartial> findByUsername(@Bind("username") String username);

    @RegisterBeanMapper(CoachPartial.class)
    @SqlQuery("selectById")
    Optional<CoachPartial> findById(@Bind("id") UUID id);

    @RegisterBeanMapper(value = AthleteDetail.class, prefix = "au")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "ac")
    @UseRowReducer(AthleteDetailRowReducer.class)
    @SqlQuery("selectAllAthletesAssignedToCoach")
    @AllowUnusedBindings
    List<AthleteDetail> selectAllAthletesAssignedToCoach(@Bind("coachId") UUID coachId);

    @RegisterBeanMapper(value = AthleteDetail.class, prefix = "au")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "ac")
    @UseRowReducer(AthleteDetailRowReducer.class)
    @SqlQuery("selectAthleteAssignedToCoach")
    @AllowUnusedBindings
    List<AthleteDetail> selectAthleteAssignedToCoach(@Bind("coachId") UUID coachId, @Bind("athleteId") UUID athleteId);
}
