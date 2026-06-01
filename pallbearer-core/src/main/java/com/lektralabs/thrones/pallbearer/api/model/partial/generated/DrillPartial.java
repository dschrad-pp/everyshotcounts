package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

// This is generated code. If you modify it (and you are welcome to), please
// move out of this package and remove this comment.
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DrillPartial implements Serializable, EntityMethods {

    private Optional<UUID> drillId;
    private UUID drillItemId;
    private UUID userId;
    private Optional<UUID> mediaId;
    private String drillStatus;
    private Optional<Integer> version;
    private Integer attemptsDetected;
    private Integer attemptsReported;
    private Integer makesDetected;
    private Integer makesReported;
    private String attemptLocalId;
    private Integer hotStreak;
    private Integer coldStreak;
    private Timestamp recordedAt;
    private Timestamp startedAt;

    public DrillRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return DrillRow.builder()
                .id(optionalFactory(drillId, UUID.randomUUID()))
                .drillItemId(drillItemId)
                .userId(userId)
                .mediaId(mediaId)
                .drillStatus(drillStatus)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .attemptsDetected(attemptsDetected)
                .attemptsReported(attemptsReported)
                .makesDetected(makesDetected)
                .makesReported(makesReported)
                .build();   
    }

}
