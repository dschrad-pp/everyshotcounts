package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.reducer.AthleteDetailRowReducer;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.AllowUnusedBindings;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AthleteDao {

    @RegisterBeanMapper(value = AthleteDetail.class, prefix = "au")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "ac")
    @UseStringTemplateSqlLocator
    @SqlQuery("findByAthleteId")
    @UseRowReducer(AthleteDetailRowReducer.class)
    @AllowUnusedBindings
    Optional<AthleteDetail> findByAthleteId(UUID athleteId);

    @RegisterBeanMapper(value = AthleteDetail.class, prefix = "au")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "ac")
    @UseStringTemplateSqlLocator
    @SqlQuery("selectAll")
    @UseRowReducer(AthleteDetailRowReducer.class)
    @AllowUnusedBindings
    List<AthleteDetail> findAll(@Define("findOptions") String findOptions);

    @RegisterBeanMapper(value = AthleteDetail.class, prefix = "au")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "ac")
    @UseStringTemplateSqlLocator
    @SqlQuery("findFeaturedAthletes")
    @UseRowReducer(AthleteDetailRowReducer.class)
    @AllowUnusedBindings
    List<AthleteDetail> findFeaturedAthletes(UUID userId);

}
