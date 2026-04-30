package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.TeamPartial;
import com.lektralabs.thrones.pallbearer.jdbi.dao.TeamDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TeamRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.TeamBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class TeamService extends TeamBaseService {
    private static final Logger logger = Logger.getLogger(TeamService.class);

    private static final UUID DEFAULT_TEAM_ID = UUID.fromString("2a3b4697-26ee-4294-812e-6e1b00bd8e90");
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS  = "0123456789";

    private TeamDao teamDao;

    @PostConstruct
    public void init() {
        super.init();
        this.teamDao = jdbiProvider.getJdbi().onDemand(TeamDao.class);
    }

    public Map<String, String> createTeamWithJoinCode(String teamName, UUID coachUserId) {
        TeamRow fallback = findById(DEFAULT_TEAM_ID)
                .orElseThrow(() -> new IllegalStateException("Default team not found: " + DEFAULT_TEAM_ID));
        TeamPartial teamPartial = TeamPartial.builder()
                .sportId(fallback.getSportId())
                .organizationId(fallback.getOrganizationId())
                .name(Optional.of(teamName))
                .description(Optional.empty())
                .build();
        UUID teamId = create(teamPartial);
        String joinCode = generateUniqueJoinCode();
        teamDao.insertJoinCode(teamId, joinCode);
        mapUserToTeam(coachUserId, teamId);
        logger.infof("Coach %s created team %s with join code %s", coachUserId, teamId, joinCode);
        return Map.of("teamId", teamId.toString(), "joinCode", joinCode);
    }

    public Optional<TeamRow> joinTeamByCode(UUID userId, String joinCode) {
        if (teamDao.userHasTeam(userId)) {
            throw new IllegalStateException("ALREADY_ON_TEAM");
        }
        Optional<TeamRow> team = teamDao.findTeamByJoinCode(joinCode);
        team.ifPresent(t -> mapUserToTeam(userId, t.getId()));
        return team;
    }

    public boolean userHasTeam(UUID userId) {
        return teamDao.userHasTeam(userId);
    }

    public Optional<Map<String, String>> getTeamForAthlete(UUID athleteId) {
        return teamDao.findTeamByUserId(athleteId).map(team -> Map.of(
                "teamId", team.getId().toString(),
                "name",   team.getName().orElse("")
        ));
    }

    public boolean removeUserFromTeam(UUID teamId, UUID userId) {
        return teamDao.removeUserFromTeam(teamId, userId) > 0;
    }

    public Optional<String> getJoinCode(UUID teamId) {
        return teamDao.findJoinCodeByTeamId(teamId);
    }

    public Optional<Map<String, String>> getTeamForCoach(UUID coachId) {
        return teamDao.findTeamByUserId(coachId).map(team -> {
            String joinCode = teamDao.findJoinCodeByTeamId(team.getId()).orElseGet(() -> {
                // Old team with no join code — generate one now
                String newCode = generateUniqueJoinCode();
                teamDao.insertJoinCode(team.getId(), newCode);
                logger.infof("Auto-generated join code %s for existing team %s", newCode, team.getId());
                return newCode;
            });
            return Map.of(
                "teamId",   team.getId().toString(),
                "name",     team.getName().orElse(""),
                "joinCode", joinCode
            );
        });
    }

    private String generateUniqueJoinCode() {
        SecureRandom rng = new SecureRandom();
        String candidate;
        do {
            String letterPart = IntStream.range(0, 3)
                    .mapToObj(i -> String.valueOf(LETTERS.charAt(rng.nextInt(LETTERS.length()))))
                    .collect(Collectors.joining());
            String digitPart = IntStream.range(0, 6)
                    .mapToObj(i -> String.valueOf(DIGITS.charAt(rng.nextInt(DIGITS.length()))))
                    .collect(Collectors.joining());
            candidate = letterPart + digitPart;
        } while (teamDao.findTeamByJoinCode(candidate).isPresent());
        return candidate;
    }

}
