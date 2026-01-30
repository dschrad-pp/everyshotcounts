package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeRow;
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
public class ChallengePartial implements Serializable, EntityMethods {

    private Optional<UUID> challengeId;
    private Optional<String> name;
    private Optional<String> description;
    private Long startTime;
    private Long endTime;
    private UUID sportChallengeTypeId;
    private String challengeTypeCode;
    private String challengeVisibilityCode;
    private String allowRetryCode;
    private Integer retryMax;
    private String allowOpenInviteCode;
    private Long timeLimitMs;
    private Optional<Integer> version;
    private Optional<BigDecimal> costOfEntry;
    private Optional<Integer> numberOfShots;
    private Boolean personalizedIntro;

    public ChallengeRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  ChallengeRow.builder()
                .id(optionalFactory(challengeId, UUID.randomUUID()))
                .name(name)
                .description(description)
                .startTime(startTime)
                .endTime(endTime)
                .sportChallengeTypeId(sportChallengeTypeId)
                .challengeTypeCode(challengeTypeCode)
                .challengeVisibilityCode(challengeVisibilityCode)
                .allowRetryCode(allowRetryCode)
                .retryMax(retryMax)
                .allowOpenInviteCode(allowOpenInviteCode)
                .timeLimitMs(timeLimitMs)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .costOfEntry(costOfEntry)
                .numberOfShots(numberOfShots)
                .personalizedIntro(personalizedIntro)
                .build();
    }


}