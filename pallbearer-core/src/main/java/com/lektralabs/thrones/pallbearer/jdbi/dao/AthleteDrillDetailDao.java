package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.reducer.AthleteDrillDetailRowReducer;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;


import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AthleteDrillDetailDao {

    @RegisterBeanMapper(value = AthleteDrillDetail.class, prefix = "di")
    @RegisterBeanMapper(value = DrillDetail.class, prefix = "dr")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = DrillGroupRow.class, prefix = "dg")
    @UseStringTemplateSqlLocator
    @SqlQuery("getByAthleteAndDrillItem")
    @UseRowReducer(AthleteDrillDetailRowReducer.class)
    Optional<AthleteDrillDetail> getByAthleteAndDrillItem(UUID athleteUserId,
                                                          UUID drillItemId);

    @RegisterBeanMapper(value = AthleteDrillDetail.class, prefix = "di")
    @RegisterBeanMapper(value = DrillDetail.class, prefix = "dr")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = DrillGroupRow.class, prefix = "dg")
    @UseStringTemplateSqlLocator
    @SqlQuery("getByAthleteAndGroup")
    @UseRowReducer(AthleteDrillDetailRowReducer.class)
    List<AthleteDrillDetail> getByAthleteAndGroup(UUID athleteUserId,
                                                  UUID drillGroupId,
                                                  FindOptions findOptions);

    @RegisterBeanMapper(value = AthleteDrillDetail.class, prefix = "di")
    @RegisterBeanMapper(value = DrillDetail.class, prefix = "dr")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = DrillGroupRow.class, prefix = "dg")
    @UseStringTemplateSqlLocator
    @SqlQuery("getByTeamTimeline")
    @UseRowReducer(AthleteDrillDetailRowReducer.class)
    List<AthleteDrillDetail> getByTeamTimeline(UUID teamId,
                                               FindOptions findOptions);

    @RegisterBeanMapper(value = AthleteDrillDetail.class, prefix = "di")
    @RegisterBeanMapper(value = DrillDetail.class, prefix = "dr")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = DrillGroupRow.class, prefix = "dg")
    @UseStringTemplateSqlLocator
    @SqlQuery("getByAthleteTimeline")
    @UseRowReducer(AthleteDrillDetailRowReducer.class)
    List<AthleteDrillDetail> getByAthleteTimeline(UUID athleteUserId,
                                                  FindOptions findOptions);

    @RegisterBeanMapper(value = AthleteDrillDetail.class, prefix = "di")
    @RegisterBeanMapper(value = DrillDetail.class, prefix = "dr")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = DrillGroupRow.class, prefix = "dg")
    @UseStringTemplateSqlLocator
    @SqlQuery("getByAthleteGroupAndLevel")
    @UseRowReducer(AthleteDrillDetailRowReducer.class)
    List<AthleteDrillDetail> getByAthleteGroupAndLevel(UUID athleteUserId,
                                                     UUID drillGroupId,
                                                     Integer levelIndex,
                                                     FindOptions findOptions);

    @RegisterBeanMapper(value = AthleteDrillDetail.class, prefix = "di")
    @RegisterBeanMapper(value = DrillDetail.class, prefix = "dr")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = DrillGroupRow.class, prefix = "dg")
    @UseStringTemplateSqlLocator
    @SqlQuery("getCompletedByAthleteWithFilters")
    @UseRowReducer(AthleteDrillDetailRowReducer.class)
    List<AthleteDrillDetail> getCompletedByAthleteWithFilters(
            @Bind("athleteUserId") UUID athleteUserId,
            @Define("tagCodes") @BindList(value = "tagCodes", onEmpty = BindList.EmptyHandling.NULL) List<String> tagCodes,
            @Bind("limit") int limit,
            @Bind("offset") int offset);
}
