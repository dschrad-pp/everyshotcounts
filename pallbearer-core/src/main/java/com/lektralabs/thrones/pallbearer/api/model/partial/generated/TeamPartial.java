package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TeamRow;
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
public class TeamPartial implements Serializable, EntityMethods {

    private Optional<UUID> teamId;
    private UUID sportId;
    private UUID organizationId;
    private Optional<String> name;
    private Optional<String> description;

    public TeamRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  TeamRow.builder()
                .id(optionalFactory(teamId, UUID.randomUUID()))
                .sportId(sportId)
                .organizationId(organizationId)
                .name(name)
                .description(description)
                .build();
    }


}