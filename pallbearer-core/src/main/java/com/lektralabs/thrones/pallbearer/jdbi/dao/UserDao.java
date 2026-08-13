package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.UserDeletionStateRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.Optional;
import java.util.UUID;

public interface UserDao {

    @SqlUpdate("INSERT INTO t_user_role_xref (user_id, role_id) VALUES (:userId, :roleId) ON CONFLICT DO NOTHING")
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

    /**
     * Deletion-grace fields only — for the per-request filter, which must not
     * pay for the avatar blob {@code selectByUsername} carries.
     */
    @RegisterBeanMapper(UserDeletionStateRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectDeletionStateByUsername")
    Optional<UserDeletionStateRow> findDeletionStateByUsername(String username);

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

    /**
     * Sets or clears the account-deletion grace-period flags. Pass both values to mark the
     * account pending deletion, or both nulls to restore it. Deliberately not part of the
     * versioned {@code update} statement so login-time row updates can't race the flags.
     */
    @UseStringTemplateSqlLocator
    @SqlUpdate("updateDeletionState")
    int updateDeletionState(@Bind("id") UUID id,
            @Bind("deletionRequestedAt") Long deletionRequestedAt,
            @Bind("purgeAfter") Long purgeAfter);

    /** Users whose grace period has ended (purge_after <= now) — input for the purge cron. */
    @RegisterBeanMapper(UserRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectPendingPurge")
    java.util.List<UserRow> findPendingPurge(@Bind("now") long now);

    @UseStringTemplateSqlLocator
    @SqlUpdate("updateMetadata")
    int updateMetadata(@Bind("userId") UUID userId, @Bind("metadata") String metadata);

    @RegisterBeanMapper(com.lektralabs.thrones.pallbearer.jdbi.model.BasicUserRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectAllBasic")
    java.util.List<com.lektralabs.thrones.pallbearer.jdbi.model.BasicUserRow> findAllBasic();

}
