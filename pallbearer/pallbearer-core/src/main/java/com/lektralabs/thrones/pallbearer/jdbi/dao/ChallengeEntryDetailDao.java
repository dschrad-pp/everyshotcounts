package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeEntryDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.reducer.ChallengeEntryDetailRowReducer;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeEntryDetailDao {

    @RegisterBeanMapper(value = ChallengeEntryDetail.class, prefix = "ce")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByChallengeEntryId")
    @UseRowReducer(ChallengeEntryDetailRowReducer.class)
    Optional<ChallengeEntryDetail> getByChallengeEntryId(UUID id);

    @RegisterBeanMapper(value = ChallengeEntryDetail.class, prefix = "ce")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @UseStringTemplateSqlLocator
    @SqlQuery("selectByChallenge")
    @UseRowReducer(ChallengeEntryDetailRowReducer.class)
    List<ChallengeEntryDetail> getByChallengeId(UUID challengeId);
}
