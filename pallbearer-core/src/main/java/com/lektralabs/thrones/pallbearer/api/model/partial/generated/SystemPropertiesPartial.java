package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SystemPropertiesRow;
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
public class SystemPropertiesPartial implements Serializable, EntityMethods {

    private Optional<UUID> systemPropertiesId;
    private String propertyKey;
    private String propertyValue;

    public SystemPropertiesRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  SystemPropertiesRow.builder()
                .id(optionalFactory(systemPropertiesId, UUID.randomUUID()))
                .propertyKey(propertyKey)
                .propertyValue(propertyValue)
                .build();
    }


}