package com.lektralabs.thrones.pallbearer.jdbi.dao;

import com.lektralabs.thrones.pallbearer.jdbi.model.CoachNotificationRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@UseStringTemplateSqlLocator
public interface CoachNotificationDao {

    @SqlUpdate("insert")
    void insert(@BindBean CoachNotificationRow row);

    @RegisterBeanMapper(CoachNotificationRow.class)
    @SqlQuery("findById")
    Optional<CoachNotificationRow> findById(@Bind("id") UUID id);

    @RegisterBeanMapper(CoachNotificationRow.class)
    @SqlQuery("findByCoachIdPaginated")
    List<CoachNotificationRow> findByCoachIdPaginated(
            @Bind("coachId") UUID coachId,
            @Bind("limit") int limit,
            @Bind("offset") int offset);

    @SqlQuery("countByCoachId")
    int countByCoachId(@Bind("coachId") UUID coachId);

    @SqlQuery("countUnreadByCoachId")
    int countUnreadByCoachId(@Bind("coachId") UUID coachId);

    @SqlUpdate("markAsRead")
    int markAsRead(@Bind("id") UUID id, @Bind("callerId") UUID callerId);

    @SqlUpdate("dismiss")
    int dismiss(@Bind("id") UUID id, @Bind("callerId") UUID callerId);

    @SqlUpdate("dismissAllByCoachId")
    int dismissAllByCoachId(@Bind("coachId") UUID coachId);
}
