package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.item.EscActionItem;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;

import com.lektralabs.thrones.pallbearer.jdbi.reducer.OptionalUUIDMapper;

@RegisterColumnMapper(OptionalUUIDMapper.class)
public interface EscLeagueAppsMemberActionDao {

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateActions")
    int updateActions(long lastUpdated);

    @UseStringTemplateSqlLocator
    @SqlQuery("getActionItems")
    @RegisterBeanMapper(EscActionItem.class)
    List<EscActionItem> getActionItems();

    @UseStringTemplateSqlLocator
    @SqlQuery("getAllActionItems")
    @RegisterBeanMapper(EscActionItem.class)
    List<EscActionItem> getAllActionItems();

}
