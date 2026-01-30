package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.EscLeagueAppsMemberActionRow;
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
public class EscLeagueAppsMemberActionPartial implements Serializable, EntityMethods {

    private Optional<UUID> escLeagueAppsMemberActionId;
    private Optional<Long> memberId;
    private Optional<Long> registrationId;
    private Optional<Long> userId;
    private String usernameLa;
    private String usernameEsc;
    private Optional<String> email;
    private Optional<String> mobilePhone;
    private Optional<String> firstName;
    private Optional<String> lastName;
    private Optional<Long> birthDate;
    private Optional<String> registrationStatus;
    private Optional<String> paymentStatus;
    private String statusCode;
    private String actionCode;
    private Optional<Integer> version;

    public EscLeagueAppsMemberActionRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return EscLeagueAppsMemberActionRow.builder()
                .id(optionalFactory(escLeagueAppsMemberActionId, UUID.randomUUID()))
                .memberId(memberId)
                .registrationId(registrationId)
                .userId(userId)
                .usernameLa(usernameLa)
                .usernameEsc(usernameEsc)
                .email(email)
                .mobilePhone(mobilePhone)
                .firstName(firstName)
                .lastName(lastName)
                .birthDate(birthDate)
                .registrationStatus(registrationStatus)
                .paymentStatus(paymentStatus)
                .statusCode(StatusCode.ACTIVE.toString())
                .actionCode(actionCode)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .build();
    }

}
