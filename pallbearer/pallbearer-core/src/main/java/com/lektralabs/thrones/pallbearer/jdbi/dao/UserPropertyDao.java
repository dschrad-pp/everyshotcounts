package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPropertyDao {

    @SqlUpdate("DELETE FROM t_user_property WHERE user_id = :userId AND property_key = :key")
    @Transaction(TransactionIsolationLevel.SERIALIZABLE)
    int deleteByKey(UUID userId, String key);

    @RegisterBeanMapper(UserPropertyRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("select")
    Optional<UserPropertyRow> findById(UUID id);

    @RegisterBeanMapper(UserPropertyRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByKey")
    Optional<UserPropertyRow> findByKey(UUID userId, String propertyKey);

    @RegisterBeanMapper(UserPropertyRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByUserId")
    List<UserPropertyRow> findByUserId(UUID userId);

    @RegisterBeanMapper(UserPropertyRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByUserIds")
    List<UserPropertyRow> findByUserIds(@BindList("userIds") List<UUID> userIds);

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean UserPropertyRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("update")
    int update(@BindBean UserPropertyRow item);

    @UseStringTemplateSqlLocator
    @SqlUpdate("delete")
    int delete(UUID id);

}
