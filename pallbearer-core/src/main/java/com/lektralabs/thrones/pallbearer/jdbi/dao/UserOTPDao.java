package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.UserOTPRow;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.Optional;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;

public interface UserOTPDao {

    @UseStringTemplateSqlLocator
    @SqlUpdate("insert")
    int insert(@BindBean UserOTPRow item);

    default boolean insertOtp(UserOTPRow item) {
        return insert(item) > 0;
    }

    @RegisterBeanMapper(UserOTPRow.class)
    @UseStringTemplateSqlLocator
    @SqlQuery("getByEmail")
    Optional<UserOTPRow> getByEmail(@Bind("email") String email);

    @UseStringTemplateSqlLocator
    @SqlUpdate("deleteByEmail")
    int deleteByEmail(@Bind("email") String email);
}
