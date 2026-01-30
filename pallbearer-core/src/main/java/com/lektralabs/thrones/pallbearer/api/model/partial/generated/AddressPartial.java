package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.AddressRow;
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
public class AddressPartial implements Serializable, EntityMethods {

    private Optional<UUID> addressId;
    private String addressType;
    private Optional<BigDecimal> geoLatitude;
    private Optional<BigDecimal> geoLongitude;
    private Optional<String> address;
    private Optional<String> city;
    private Optional<String> state;
    private Optional<String> zip;
    private Optional<Integer> version;

    public AddressRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  AddressRow.builder()
                .id(optionalFactory(addressId, UUID.randomUUID()))
                .addressType(addressType)
                .geoLatitude(geoLatitude)
                .geoLongitude(geoLongitude)
                .address(address)
                .city(city)
                .state(state)
                .zip(zip)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .build();
    }


}