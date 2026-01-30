package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.Optional;
import java.util.UUID;

public interface UserDao {

    @SqlUpdate("INSERT INTO t_user_role_xref (user_id, role_id) VALUES (:userId, :roleId)")
    @Transaction(TransactionIsolationLevel.SERIALIZABLE)
    int associateRole(UUID userId, UUID roleId);

    @RegisterBeanMapper(UserRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<UserRow> findById(UUID id);

    @RegisterBeanMapper(UserRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByEmail")
    Optional<UserRow> findByEmail(String email);

    @RegisterBeanMapper(UserRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByUsername")
    Optional<UserRow> findByUsername(String username);

    @RegisterBeanMapper(UserRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByRegistrationCode")
    Optional<UserRow> findByRegistrationCode(String registrationCode);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean UserRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean UserRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(UUID id);

    @RegisterBeanMapper(com.lektralabs.thrones.pallbearer.jdbi.model.BasicUserRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectAllBasic")
    java.util.List<com.lektralabs.thrones.pallbearer.jdbi.model.BasicUserRow> findAllBasic();

}
