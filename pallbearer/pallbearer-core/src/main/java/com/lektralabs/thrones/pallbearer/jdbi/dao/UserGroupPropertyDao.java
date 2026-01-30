package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.UserGroupPropertyRow;
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

@RegisterBeanMapper(UserGroupPropertyRow.class)
@UseStringTemplateSqlLocator
public interface UserGroupPropertyDao {

    @SqlQuery("selectById")
    Optional<UserGroupPropertyRow> findById(@Bind("id") UUID id);

    @SqlQuery("selectAllByUserAndGroup")
    List<UserGroupPropertyRow> findByUserAndGroup(@Bind("userId") UUID userId, @Bind("drillGroupId") UUID drillGroupId);

    @SqlQuery("selectAllByUserId")
    List<UserGroupPropertyRow> findByUserId(@Bind("userId") UUID userId);

    @SqlQuery("selectAllByUserIds")
    List<UserGroupPropertyRow> findByUserIds(@BindList("userIds") List<UUID> userIds);

    @SqlUpdate("insert")
    @GetGeneratedKeys("id")
    UUID insert(@BindBean UserGroupPropertyRow row);

    @SqlUpdate("update")
    int update(@BindBean UserGroupPropertyRow row);

    @SqlUpdate("delete")
    int delete(@Bind("id") UUID id);

    @SqlUpdate("deleteAllByUserIdAndGroup")
    @Transaction(TransactionIsolationLevel.SERIALIZABLE)
    int deleteByUserAndGroup(@Bind("userId") UUID userId, @Bind("drillGroupId") UUID drillGroupId);

    @SqlUpdate("deleteByUserIdAndKey")
    void deleteByKey(@Bind("userId") UUID userId, @Bind("key") String key);

    // Changed to use StringTemplate instead of inline SQL
    @SqlQuery("selectByUserGroupAndKey")
    Optional<UserGroupPropertyRow> findByKey(@Bind("userId") UUID userId, @Bind("drillGroupId") UUID drillGroupId, @Bind("key") String key);

}
