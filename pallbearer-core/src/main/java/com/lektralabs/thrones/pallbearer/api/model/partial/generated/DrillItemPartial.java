package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
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
public class DrillItemPartial implements Serializable, EntityMethods {

    private Optional<UUID> drillItemId;
    private UUID drillGroupId;
    private Optional<String> name;
    private Optional<String> description;
    private Optional<UUID> mediaId;
    private Integer levelIndex;
    private Integer drillItemOrder;
    private Integer passingScore;
    private String visibilityCode;
    private String allowRetryCode;
    private Integer retryMax;
    private Long timeLimitMs;
    private Optional<Integer> version;
    private Boolean levelTest;
    private UUID teamId;
    private Integer shotsMax;

    public DrillItemRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  DrillItemRow.builder()
                .id(optionalFactory(drillItemId, UUID.randomUUID()))
                .drillGroupId(drillGroupId)
                .name(name)
                .description(description)
                .mediaId(mediaId)
                .levelIndex(levelIndex)
                .drillItemOrder(drillItemOrder)
                .passingScore(passingScore)
                .visibilityCode(visibilityCode)
                .allowRetryCode(allowRetryCode)
                .retryMax(retryMax)
                .timeLimitMs(timeLimitMs)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .levelTest(levelTest)
                .teamId(teamId)
                .shotsMax(shotsMax)
                .build();
    }


}