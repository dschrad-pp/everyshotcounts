package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeEntryRow;
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
public class ChallengeEntryPartial implements Serializable, EntityMethods {

    private Optional<UUID> challengeEntryId;
    private UUID challengeId;
    private UUID tokenId;
    private UUID mediaId;
    private Integer score;
    private Optional<Integer> version;
    private String statusCode;

    public ChallengeEntryRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  ChallengeEntryRow.builder()
                .id(optionalFactory(challengeEntryId, UUID.randomUUID()))
                .challengeId(challengeId)
                .tokenId(tokenId)
                .mediaId(mediaId)
                .score(score)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .statusCode(StatusCode.ACTIVE.toString())
                .build();
    }


}