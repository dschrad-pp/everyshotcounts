package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeParticipantDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.reducer.ChallengeParticipantDetailRowReducer;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.UUID;

public interface ChallengeParticipantDetailDao {

    @RegisterBeanMapper(value = ChallengeParticipantDetail.class, prefix = "cp")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "u")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "c")
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByChallengeId")
    @UseRowReducer(ChallengeParticipantDetailRowReducer.class)
    List<ChallengeParticipantDetail> selectByChallengeId(UUID challengeId);
}
