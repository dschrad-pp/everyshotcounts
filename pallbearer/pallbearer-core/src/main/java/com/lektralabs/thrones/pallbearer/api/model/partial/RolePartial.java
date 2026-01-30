package com.lektralabs.thrones.pallbearer.api.model.partial;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.RoleRow;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
public class RolePartial implements Serializable, EntityMethods {

    private Optional<UUID> roleId;
    private String name;
    private String description;
    private Integer version;

    public RoleRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  RoleRow.builder()
                .id(optionalFactory(roleId, UUID.randomUUID()))
                .name(name)
                .description(description)
                .statusCode(StatusCode.ACTIVE.toString())
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(version)
                .build();
    }


}