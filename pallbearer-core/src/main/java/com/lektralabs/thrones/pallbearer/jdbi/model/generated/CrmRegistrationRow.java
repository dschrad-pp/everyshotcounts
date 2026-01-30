package com.lektralabs.thrones.pallbearer.jdbi.model.generated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmRegistrationRow implements Serializable {

    private Long registrationId;
    private Optional<Long> userId;
    private Optional<String> username;
    private Optional<String> email;
    private Optional<String> firstName;
    private Optional<String> lastName;
    @ColumnName("phone")
    private Optional<String> phoneNumber;
    private Optional<String> role;
    private Optional<Long> teamId;
    private Optional<String> paymentStatus;
    private Optional<Long> subscriptionStartDate;
    private Optional<Long> subscriptionEndDate;
    private Long lastUpdated;
    private Optional<Timestamp> createdAt;
    private Optional<Timestamp> updatedAt;
    
    private static final long serialVersionUID = 1L;
}
