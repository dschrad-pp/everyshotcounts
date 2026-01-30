package com.lektralabs.thrones.leagueapps.model.json;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsMemberPartial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // This will ignore unknown fields like address2
public class Member {

    private String lastName;
    private long lastLogin;
    private String zipCode;
    private String gender;
    private String city;
    private String address1;
    private long dateJoined;
    private long groupId;
    private String type;
    private long userId;
    private long birthDate;
    private String firstName;
    private long lastUpdated;
    private String groupName;
    private boolean deleted;
    private String mobilePhone;
    private boolean newsletterOptIn;
    private String orgAccountRole;
    private long userProfileId;
    private long id;
    private String state;
    private String email;
    private String username;

    public LeagueAppsMemberPartial toPartial() {
        return LeagueAppsMemberPartial.builder()
                .lastName(Optional.ofNullable(lastName))
                .lastLogin(Optional.ofNullable(lastLogin))
                .zipCode(Optional.ofNullable(zipCode))
                .gender(Optional.ofNullable(gender))
                .city(Optional.ofNullable(city))
                .address1(Optional.ofNullable(address1))
                .dateJoined(Optional.ofNullable(dateJoined))
                .groupId(Optional.ofNullable(groupId))
                .type(Optional.ofNullable(type))
                .userId(Optional.ofNullable(userId))
                .birthDate(Optional.ofNullable(birthDate))
                .firstName(Optional.ofNullable(firstName))
                .lastUpdated(Optional.ofNullable(lastUpdated))
                .groupName(Optional.ofNullable(groupName))
                .deleted(Optional.ofNullable(deleted))
                .mobilePhone(Optional.ofNullable(mobilePhone))
                .newsletterOptIn(Optional.ofNullable(newsletterOptIn))
                .orgAccountRole(Optional.ofNullable(orgAccountRole))
                .userProfileId(Optional.ofNullable(userProfileId))
                .id(Optional.ofNullable(id))
                .state(Optional.ofNullable(state))
                .email(Optional.ofNullable(email))
                .username(Optional.ofNullable(username))
                .build();
    }
}
