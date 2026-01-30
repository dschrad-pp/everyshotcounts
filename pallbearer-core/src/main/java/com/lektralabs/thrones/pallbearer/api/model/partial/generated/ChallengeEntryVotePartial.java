package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeEntryVoteRow;
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
public class ChallengeEntryVotePartial implements Serializable, EntityMethods {

    private Optional<UUID> challengeEntryVoteId;
    private UUID challengeEntryId;
    private String voteTypeCode;
    private Optional<Integer> version;

    public ChallengeEntryVoteRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  ChallengeEntryVoteRow.builder()
                .id(optionalFactory(challengeEntryVoteId, UUID.randomUUID()))
                .challengeEntryId(challengeEntryId)
                .voteTypeCode(voteTypeCode)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .build();
    }


}