package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.SportChallengeTypeDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ChallengeEntryItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.SportItem;
import com.lektralabs.thrones.pallbearer.jdbi.reducer.ChallengeDetailRowReducer;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeDetailDao {

    @RegisterBeanMapper(value = ChallengeDetail.class, prefix = "c")
    @RegisterBeanMapper(value = ChallengeEntryItem.class, prefix = "ce")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = SportChallengeTypeDetail.class, prefix = "sct")
    @RegisterBeanMapper(value = SportItem.class, prefix = "s")
    @UseStringTemplateSqlLocator
    @SqlQuery("getByChallengeId")
    @UseRowReducer(ChallengeDetailRowReducer.class)
    Optional<ChallengeDetail> getByChallengeId(UUID challengeId, UUID currentUserId);

    @RegisterBeanMapper(value = ChallengeDetail.class, prefix = "c")
    @RegisterBeanMapper(value = ChallengeEntryItem.class, prefix = "ce")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = SportChallengeTypeDetail.class, prefix = "sct")
    @RegisterBeanMapper(value = SportItem.class, prefix = "s")
    @UseStringTemplateSqlLocator
    @SqlQuery("getFeaturedChallenges")
    @UseRowReducer(ChallengeDetailRowReducer.class)
    List<ChallengeDetail> getFeaturedChallenges(UUID currentUserId);

    @RegisterBeanMapper(value = ChallengeDetail.class, prefix = "c")
    @RegisterBeanMapper(value = ChallengeEntryItem.class, prefix = "ce")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = SportChallengeTypeDetail.class, prefix = "sct")
    @RegisterBeanMapper(value = SportItem.class, prefix = "s")
    @UseStringTemplateSqlLocator
    @SqlQuery("getFeaturedAthleteChallenge")
    @UseRowReducer(ChallengeDetailRowReducer.class)
    Optional<ChallengeDetail> getFeaturedAthleteChallenge(UUID athleteUserId, UUID currentUserId);

    @RegisterBeanMapper(value = ChallengeDetail.class, prefix = "c")
    @RegisterBeanMapper(value = ChallengeEntryItem.class, prefix = "ce")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = SportChallengeTypeDetail.class, prefix = "sct")
    @RegisterBeanMapper(value = SportItem.class, prefix = "s")
    @UseStringTemplateSqlLocator
    @SqlQuery("getAthleteChallengeDetails")
    @UseRowReducer(ChallengeDetailRowReducer.class)
    List<ChallengeDetail> getAthleteChallengeDetails(UUID athleteUserId, UUID currentUserId);

    @RegisterBeanMapper(value = ChallengeDetail.class, prefix = "c")
    @RegisterBeanMapper(value = ChallengeEntryItem.class, prefix = "ce")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "cu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "cc")
    @RegisterBeanMapper(value = UserDetail.class, prefix = "mu")
    @RegisterBeanMapper(value = ContactItem.class, prefix = "mc")
    @RegisterBeanMapper(value = SportChallengeTypeDetail.class, prefix = "sct")
    @RegisterBeanMapper(value = SportItem.class, prefix = "s")
    @UseStringTemplateSqlLocator
    @SqlQuery("getFanChallengeDetails")
    @UseRowReducer(ChallengeDetailRowReducer.class)
    List<ChallengeDetail> getFanChallengeDetails(UUID fanUserId, UUID currentUserId);
}
