package com.lektralabs.thrones.pallbearer.jdbi.model.generated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillAttemptHistoryRow implements Serializable {

    private UUID id;
    private UUID userId;
    private UUID drillId;
    private Integer attemptsDetected;
    private Integer attemptsReported;
    private Integer makesDetected;
    private Integer makesReported;
    private Timestamp recordedAt; // matches recorded_at TIMESTAMP in DB
    private Integer version;
    private UUID mediaId;

    private static final long serialVersionUID = 1L;

}
