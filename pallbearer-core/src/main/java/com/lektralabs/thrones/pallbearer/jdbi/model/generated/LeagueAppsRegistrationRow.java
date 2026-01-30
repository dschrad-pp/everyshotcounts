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
public class LeagueAppsRegistrationRow implements Serializable {

    private Optional<String> zipCode;
    private Optional<String> registrationStatus;
    private Optional<String> parentEmail;
    private Optional<Long> registrationStartDate;
    private Optional<Long> groupId;
    private Optional<String> paymentPlanStatus;
    private Optional<Long> masterProgramId;
    private Optional<Long> lastUpdated;
    private Optional<Long> price;
    private Optional<String> programName;
    private Optional<Long> registrationId;
    private Optional<String> season;
    private Long id;
    private Optional<String> state;
    private Optional<String> email;
    private Optional<Long> outstandingBalance;
    private Optional<Long> isStaff;
    private Optional<Long> created;
    private Optional<Long> parentUserId;
    private Optional<Long> waiverAcceptedTimestamp;
    private Optional<String> firstName;
    private Optional<String> parentFirstName;
    private Optional<String> groupName;
    private Optional<Long> userProfileId;
    private Optional<String> userType;
    private Optional<String> lastName;
    private Optional<String> role;
    private Optional<String> gender;
    private Optional<String> city;
    private Optional<String> siteName;
    private Optional<String> parentPhone;
    private Optional<Double> amountPaid;
    private Optional<Long> registrationEndDate;
    private Optional<String> programState;
    private Optional<Long> programEndDate;
    private Optional<String> currentGradeLevel;
    private Optional<Long> lastPaymentDate;
    private Optional<String> paymentStatus;
    private Optional<String> paymentPlan;
    private Optional<String> programType;
    private Optional<String> parentLastName;
    private Optional<String> address1;
    private Optional<Double> totalAmountDue;
    private Optional<String> userName;
    private Optional<Long> userId;
    private Optional<Long> birthDate;
    private Optional<Boolean> isCoCaptain;
    private Optional<Long> sportId;
    private Optional<String> phone;
    private Optional<String> currentClubTeamAffiliation;
    private Optional<String> masterProgramName;
    private Optional<String> generalTermsOfService;
    private Optional<Long> teamId;
    private Optional<String> currentSchoolAttending;
    private Optional<Long> invoiceId;
    private Optional<Long> programStartDate;
    private Optional<String> sport;
    private Optional<Long> programId;
    private static final long serialVersionUID = 1L;

}