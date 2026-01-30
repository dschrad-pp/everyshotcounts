package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeTokenRow;
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
public class ChallengeTokenPartial implements Serializable, EntityMethods {

    private Optional<UUID> challengeTokenId;
    private UUID challengeId;
    private UUID userId;
    private String tokenStatusCode;
    private String tokenTypeCode;

    public ChallengeTokenRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  ChallengeTokenRow.builder()
                .id(optionalFactory(challengeTokenId, UUID.randomUUID()))
                .challengeId(challengeId)
                .userId(userId)
                .tokenStatusCode(tokenStatusCode)
                .tokenTypeCode(tokenTypeCode)
                .build();
    }


}