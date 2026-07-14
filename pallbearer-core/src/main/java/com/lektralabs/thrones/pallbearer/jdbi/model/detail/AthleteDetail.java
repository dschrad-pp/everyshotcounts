package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthleteDetail {

    private UUID userId;

    private String email;

    private String userAlias;

    private String username;

    private ContactItem contactItem;

    private Map<String, String> userProperties;

    private Map<String, Map<String, String>> groupProperties;

    /**
     * ISO8601 UTC instant of the athlete's most recent drill completion
     * (MAX(recorded_at) over t_drill_attempt_history), e.g. 2026-07-14T09:00:00Z.
     * Null when the athlete has never submitted a completion — the iOS roster
     * hides its "last active" badge for null. Populated only by the coach
     * roster path ({@code findAllAthletesAssignedToCoach}).
     */
    private String lastActiveAt;
}
