package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
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
public class DrillGroupPartial implements Serializable, EntityMethods {

    private Optional<UUID> drillGroupId;
    private UUID teamId;
    private Optional<String> name;
    private Optional<String> description;
    private Integer drillGroupOrder;
    private String drillGroupTypeCode;
    private Optional<Integer> version;

    public DrillGroupRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  DrillGroupRow.builder()
                .id(optionalFactory(drillGroupId, UUID.randomUUID()))
                .teamId(teamId)
                .name(name)
                .description(description)
                .drillGroupOrder(drillGroupOrder)
                .drillGroupTypeCode(drillGroupTypeCode)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .build();
    }


}