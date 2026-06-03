package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.CoachNotificationDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.TeamDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.CoachNotificationRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.DeviceTokenRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ContactRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TeamRow;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CoachNotificationService {

    private static final Logger logger = Logger.getLogger(CoachNotificationService.class);

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserService userService;

    @Inject
    ContactService contactService;

    @Inject
    DrillItemService drillItemService;

    @Inject
    DeviceTokenService deviceTokenService;

    @Inject
    ApnsService apnsService;

    @Inject
    TeamService teamService;

    private CoachNotificationDao coachNotificationDao;
    private TeamDao teamDao;

    @PostConstruct
    public void init() {
        this.coachNotificationDao = jdbiProvider.getJdbi().onDemand(CoachNotificationDao.class);
        this.teamDao = jdbiProvider.getJdbi().onDemand(TeamDao.class);
    }

    public void createNotificationForDrillCompletion(UUID athleteId, UUID drillId, UUID drillItemId, Integer makesDetected) {
        try {
            Optional<TeamRow> teamOpt = teamDao.findTeamByUserId(athleteId);
            if (teamOpt.isEmpty()) {
                logger.debugf("Athlete %s has no team — skipping coach notification", athleteId);
                return;
            }

            Optional<UUID> coachIdOpt = teamDao.findCoachIdByTeamId(teamOpt.get().getId());
            if (coachIdOpt.isEmpty()) {
                logger.debugf("Team %s has no coach — skipping notification", teamOpt.get().getId());
                return;
            }
            UUID coachId = coachIdOpt.get();

            String athleteFirstName = "";
            String athleteLastName = "";
            Optional<UserRow> athleteUserOpt = userService.findById(athleteId);
            if (athleteUserOpt.isPresent()) {
                Optional<ContactRow> contactOpt = contactService.findById(athleteUserOpt.get().getContactId());
                if (contactOpt.isPresent()) {
                    athleteFirstName = contactOpt.get().getFirstName();
                    athleteLastName = contactOpt.get().getLastName();
                }
            }

            String drillName = "";
            Integer passingScore = null;
            Optional<DrillItemRow> drillItemOpt = drillItemService.findById(drillItemId);
            if (drillItemOpt.isPresent()) {
                drillName = drillItemOpt.get().getName().orElse("");
                passingScore = drillItemOpt.get().getPassingScore();
            }

            if (passingScore != null && makesDetected != null && makesDetected < passingScore) {
                logger.debugf("Athlete %s did not pass drill %s (makes=%d, required=%d) — skipping coach notification",
                        athleteId, drillItemId, makesDetected, passingScore);
                return;
            }

            long now = System.currentTimeMillis();
            CoachNotificationRow notification = CoachNotificationRow.builder()
                    .id(UUID.randomUUID())
                    .coachId(coachId)
                    .athleteId(athleteId)
                    .drillId(drillId)
                    .drillItemId(drillItemId)
                    .drillName(drillName)
                    .athleteFirstName(athleteFirstName)
                    .athleteLastName(athleteLastName)
                    .completedAt(now)
                    .isRead(false)
                    .isDismissed(false)
                    .creationDate(now)
                    .build();
            coachNotificationDao.insert(notification);

            Optional<DeviceTokenRow> tokenOpt = deviceTokenService.findByUserId(coachId, "ios");
            if (tokenOpt.isPresent()) {
                apnsService.sendDrillCompletionNotification(
                        tokenOpt.get().getToken(),
                        athleteFirstName,
                        athleteLastName,
                        drillName,
                        athleteId,
                        drillId,
                        drillItemId,
                        () -> deviceTokenService.deleteByUserId(coachId, "ios")
                );
            }
        } catch (Exception e) {
            logger.warnf(e, "Failed to create coach notification for drill completion: athleteId=%s drillId=%s", athleteId, drillId);
        }
    }

    public List<CoachNotificationRow> findByCoachId(UUID coachId, int page, int limit) {
        int offset = (page - 1) * limit;
        return coachNotificationDao.findByCoachIdPaginated(coachId, limit, offset);
    }

    public int countByCoachId(UUID coachId) {
        return coachNotificationDao.countByCoachId(coachId);
    }

    public int countUnreadByCoachId(UUID coachId) {
        return coachNotificationDao.countUnreadByCoachId(coachId);
    }

    public int markAsRead(UUID notificationId, UUID callerId) {
        return coachNotificationDao.markAsRead(notificationId, callerId);
    }

    public int dismiss(UUID notificationId, UUID callerId) {
        return coachNotificationDao.dismiss(notificationId, callerId);
    }

    public int dismissAll(UUID coachId) {
        return coachNotificationDao.dismissAllByCoachId(coachId);
    }
}
