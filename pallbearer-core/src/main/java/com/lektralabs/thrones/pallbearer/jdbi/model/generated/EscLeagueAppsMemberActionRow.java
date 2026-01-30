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
public class EscLeagueAppsMemberActionRow implements Serializable {

    private UUID id;
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
    private Long creationDate;
    private Long modificationDate;
    private UUID createdById;
    private UUID modifiedById;
    private Integer version;
    private static final long serialVersionUID = 1L;

}