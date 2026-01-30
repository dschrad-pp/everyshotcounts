package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.UUID;

public interface CurrentUserDao {

    @RegisterBeanMapper(value = CurrentUser.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("findById")
    CurrentUser findById(UUID id);


    @RegisterBeanMapper(value = CurrentUser.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("findByName")
    CurrentUser findByName(String username);

}
