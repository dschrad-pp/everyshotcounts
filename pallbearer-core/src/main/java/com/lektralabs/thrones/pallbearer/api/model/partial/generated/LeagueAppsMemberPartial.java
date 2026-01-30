package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsMemberRow;
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
public class LeagueAppsMemberPartial implements Serializable, EntityMethods {

    private Optional<String> lastName;
    private Optional<Long> lastLogin;
    private Optional<String> zipCode;
    private Optional<String> gender;
    private Optional<String> city;
    private Optional<String> address1;
    private Optional<Long> dateJoined;
    private Optional<Long> groupId;
    private Optional<String> type;
    private Optional<Long> userId;
    private Optional<Long> birthDate;
    private Optional<String> firstName;
    private Optional<Long> lastUpdated;
    private Optional<String> groupName;
    private Optional<Boolean> deleted;
    private Optional<String> mobilePhone;
    private Optional<Boolean> newsletterOptIn;
    private Optional<String> orgAccountRole;
    private Optional<Long> userProfileId;
    private Optional<Long> id;
    private Optional<String> state;
    private Optional<String> email;
    private Optional<String> username;

    public LeagueAppsMemberRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  LeagueAppsMemberRow.builder()
                .lastName(lastName)
                .lastLogin(lastLogin)
                .zipCode(zipCode)
                .gender(gender)
                .city(city)
                .address1(address1)
                .dateJoined(dateJoined)
                .groupId(groupId)
                .type(type)
                .userId(userId)
                .birthDate(birthDate)
                .firstName(firstName)
                .lastUpdated(lastUpdated)
                .groupName(groupName)
                .deleted(deleted)
                .mobilePhone(mobilePhone)
                .newsletterOptIn(newsletterOptIn)
                .orgAccountRole(orgAccountRole)
                .userProfileId(userProfileId)
                .id(id.get())
                .state(state)
                .email(email)
                .username(username)
                .build();
    }


}