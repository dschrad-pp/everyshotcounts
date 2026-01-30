package com.lektralabs.thrones.pallbearer.api.model.partial;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;


@Data
@Builder
public class UserPropertyPartial implements Serializable, EntityMethods {

    private Optional<UUID> userPropertyId;
    private UUID userId;
    private String propertyKey;
    private String propertyValue;

    public UserPropertyRow toRow() {
        return  UserPropertyRow.builder()
                .id(optionalFactory(userPropertyId, UUID.randomUUID()))
                .userId(userId)
                .propertyKey(propertyKey)
                .propertyValue(propertyValue)
                .build();
    }


}