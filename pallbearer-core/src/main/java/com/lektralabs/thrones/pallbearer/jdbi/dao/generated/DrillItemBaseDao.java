package com.lektralabs.thrones.pallbearer.jdbi.dao.generated;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
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

public interface DrillItemBaseDao {

    @RegisterBeanMapper(DrillItemRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<DrillItemRow> findById(UUID id);

    @RegisterBeanMapper(DrillItemRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectAll")
    List<DrillItemRow> findAll(@Define("findOptions") String findOptions);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean DrillItemRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean DrillItemRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(UUID id);

}
