package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.CrmRegistrationRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;

@RegisterBeanMapper(CrmRegistrationRow.class)
@UseStringTemplateSqlLocator
public interface CrmRegistrationDao {

    @SqlQuery("selectByRegistrationId")
    Optional<CrmRegistrationRow> findByRegistrationId(long registrationId);

    @SqlQuery("selectAll")
    List<CrmRegistrationRow> findAll();

    @SqlUpdate("insert")
    int insert(@BindBean CrmRegistrationRow row);

    @SqlUpdate("update")
    int update(@BindBean CrmRegistrationRow row);
}
