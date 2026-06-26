package com.lektralabs.thrones.pallbearer.jdbi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachNotificationRow implements Serializable {

    private UUID id;
    private UUID coachId;
    private UUID athleteId;
    private UUID drillId;
    private UUID drillItemId;
    private String attemptLocalId;
    private String drillName;
    private String athleteFirstName;
    private String athleteLastName;
    private Long completedAt;
    private Boolean isRead;
    private Boolean isDismissed;
    private Long creationDate;
    private Boolean scoreAdjusted;
    private Integer makesDetected;
    private Integer attemptsDetected;
    private Integer makesReported;
    private Integer attemptsReported;
    private static final long serialVersionUID = 1L;

}
