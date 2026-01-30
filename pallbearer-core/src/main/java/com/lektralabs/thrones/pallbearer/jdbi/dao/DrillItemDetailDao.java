package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.reducer.DrillItemDetailRowReducer;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.sqlobject.customizer.Bind;

public interface DrillItemDetailDao {

    @RegisterBeanMapper(value = DrillItemDetail.class, prefix = "di")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = DrillGroupRow.class, prefix = "dg")
    @UseStringTemplateSqlLocator
    @SqlQuery("getByDrillItemId")
    @UseRowReducer(DrillItemDetailRowReducer.class)
    Optional<DrillItemDetail> getByDrillItemId(UUID drillItemId);

    @RegisterBeanMapper(value = DrillItemDetail.class, prefix = "di")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = DrillGroupRow.class, prefix = "dg")
    @UseStringTemplateSqlLocator
    @SqlQuery("getTeamDrillItemDetails")
    @UseRowReducer(DrillItemDetailRowReducer.class)
    List<DrillItemDetail> getTeamDrillItemDetails(UUID teamId);

    @RegisterBeanMapper(value = DrillItemDetail.class, prefix = "di")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = DrillGroupRow.class, prefix = "dg")
    @UseStringTemplateSqlLocator
    @SqlQuery("findDrillItemByGroupId")
    @UseRowReducer(DrillItemDetailRowReducer.class)
    List<DrillItemDetail> findDrillItemByGroupId(UUID groupId);
}
