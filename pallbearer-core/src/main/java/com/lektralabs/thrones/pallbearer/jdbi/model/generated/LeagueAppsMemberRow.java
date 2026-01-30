package com.lektralabs.thrones.pallbearer.jdbi.model.generated;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueAppsMemberRow implements Serializable {

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
    private Long id;
    private Optional<String> state;
    private Optional<String> email;
    private Optional<String> username;
    private static final long serialVersionUID = 1L;

}