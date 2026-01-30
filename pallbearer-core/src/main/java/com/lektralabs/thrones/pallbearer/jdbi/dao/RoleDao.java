package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.RoleRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleDao {

    @RegisterBeanMapper(RoleRow.class)
    @SqlQuery("SELECT * FROM t_role WHERE name = :name")
    List<RoleRow> findByName(String name);

    @RegisterBeanMapper(RoleRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<RoleRow> findById(UUID id);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean RoleRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean RoleRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(UUID id);

}
