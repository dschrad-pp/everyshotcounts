package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsMemberActionRow;
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
public class LeagueAppsMemberActionPartial implements Serializable, EntityMethods {

    private Optional<Long> id;
    private Optional<Long> memberId;
    private Optional<String> email;
    private Optional<String> username;
    private String statusCode;
    private String actionCode;
    private Optional<Integer> version;

    public LeagueAppsMemberActionRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  LeagueAppsMemberActionRow.builder()
                .id(id.get())
                .memberId(memberId)
                .email(email)
                .username(username)
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