package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.FinancialAccountRow;
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
public class FinancialAccountPartial implements Serializable, EntityMethods {

    private Optional<UUID> financialAccountId;
    private UUID userId;
    private Optional<String> financialAccountType;
    private Optional<String> username;
    private Optional<String> clientKey;
    private Optional<String> clientSecret;
    private Optional<String> allowedAccess;
    private String statusCode;
    private Optional<Integer> version;

    public FinancialAccountRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  FinancialAccountRow.builder()
                .id(optionalFactory(financialAccountId, UUID.randomUUID()))
                .userId(userId)
                .financialAccountType(financialAccountType)
                .username(username)
                .clientKey(clientKey)
                .clientSecret(clientSecret)
                .allowedAccess(allowedAccess)
                .statusCode(StatusCode.ACTIVE.toString())
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .build();
    }


}