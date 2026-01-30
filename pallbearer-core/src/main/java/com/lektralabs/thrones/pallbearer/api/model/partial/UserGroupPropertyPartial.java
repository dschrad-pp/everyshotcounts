package com.lektralabs.thrones.pallbearer.api.model.partial;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserGroupPropertyRow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupPropertyPartial implements Serializable, EntityMethods {

    private Optional<UUID> propertyId;
    private UUID userId;
    private UUID groupId;
    private String propertyKey;
    private String propertyValue;

    public UserGroupPropertyRow toRow() {
        return UserGroupPropertyRow.builder()
                .id(optionalFactory(propertyId, UUID.randomUUID()))
                .userId(userId)
                .drillGroupId(groupId)
                .propertyKey(propertyKey)
                .propertyValue(propertyValue)
                .build();
    }
}
