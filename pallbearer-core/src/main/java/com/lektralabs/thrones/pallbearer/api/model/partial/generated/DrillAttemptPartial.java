package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

// This is generated code. If you modify it (and you are welcome to), please
// move out of this package and remove this comment.

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DrillAttemptPartial implements Serializable, EntityMethods {

    private Optional<UUID> drillAttemptId;
    private UUID drillId;
    private UUID mediaId;
    private Long startTime;
    private Long endTime;
    private Long elapsedTime;
    private Integer score;
    private Integer retries;
    private String drillAttemptStatus;
    private Optional<Integer> version;

    public DrillAttemptRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  DrillAttemptRow.builder()
                .id(optionalFactory(drillAttemptId, UUID.randomUUID()))
                .drillId(drillId)
                .mediaId(mediaId)
                .startTime(startTime)
                .endTime(endTime)
                .elapsedTime(elapsedTime)
                .score(score)
                .retries(retries)
                .drillAttemptStatus(drillAttemptStatus)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .build();
    }


}