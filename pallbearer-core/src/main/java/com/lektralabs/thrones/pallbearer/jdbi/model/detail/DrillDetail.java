package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.UUID;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillAttemptHistoryResponse;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillDetail {

    private UUID id;
    private UUID drillItemId;
    private UUID userId;
    private Optional<UUID> mediaId;
    private Optional<String> mediaStatus;
    private String drillStatus;
    private Long creationDate;
    private Long modificationDate;
    private UserDetail createdByUserDetail;
    private UserDetail modifiedByUserDetail;
    private Integer version;
    private Integer attemptsDetected;
    private Integer attemptsReported;
    private Integer makesDetected;
    private Integer makesReported;
    private List<DrillAttemptHistoryResponse> attemptHistory;

    public Optional<DrillAttemptHistoryResponse> getMostRecentAttempt() {
        if (attemptHistory == null || attemptHistory.isEmpty()) {
            return Optional.empty();
        }
        return attemptHistory.stream()
                .max((a1, a2) -> a1.getRecordedAt().compareTo(a2.getRecordedAt()));
    }
}
