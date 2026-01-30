package com.lektralabs.thrones.leagueapps.model.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsRegistrationPartial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Registration {
    private String zipCode;
    private String registrationStatus;
    private String parentEmail;
    private long registrationStartDate;
    private long groupId;
    private String paymentPlanStatus;
    private long masterProgramId;
    private long lastUpdated;
    private long price;
    private String programName;
    private long registrationId;
    private String season;
    private long id;
    private String state;
    private String email;
    private long outstandingBalance;
    private long isStaff;
    private long created;
    private long parentUserId;
    private String team;
    private long waiverAcceptedTimestamp;
    private String firstName;
    private String parentFirstName;
    private String groupName;
    private long userProfileId;
    private String userType;
    private String lastName;
    private String role;
    private String gender;
    private String city;
    private String siteName;
    private String parentPhone;
    private double amountPaid;
    private long registrationEndDate;
    private String programState;
    private long programEndDate;
    @JsonProperty("Current Grade Level")
    private String currentGradeLevel;
    private long lastPaymentDate;
    private String paymentStatus;
    private String paymentPlan;
    private String programType;
    private String parentLastName;
    private String address1;
    private double totalAmountDue;
    private String userName;
    private long userId;
    private long birthDate;
    @JsonProperty("isCoCaptain")
    private boolean isCoCaptain;
    private long sportId;
    private String phone;
    @JsonProperty("Current Club Team Affiliation")
    private String currentClubTeamAffiliation;
    private String masterProgramName;
    @JsonProperty("General Terms of Service for the Every Shot Counts Basketball Training Program")
    private String generalTermsOfService;
    private long teamId;
    @JsonProperty("Current School Attending")
    private String currentSchoolAttending;
    private long invoiceId;
    private long programStartDate;
    private String sport;
    private long programId;

    public LeagueAppsRegistrationPartial toPartial() {
        return LeagueAppsRegistrationPartial.builder()
                .zipCode(Optional.ofNullable(zipCode))
                .registrationStatus(Optional.ofNullable(registrationStatus))
                .parentEmail(Optional.ofNullable(parentEmail))
                .registrationStartDate(Optional.ofNullable(registrationStartDate))
                .groupId(Optional.ofNullable(groupId))
                .paymentPlanStatus(Optional.ofNullable(paymentPlanStatus))
                .masterProgramId(Optional.ofNullable(masterProgramId))
                .lastUpdated(Optional.ofNullable(lastUpdated))
                .price(Optional.ofNullable(price))
                .programName(Optional.ofNullable(programName))
                .registrationId(Optional.ofNullable(registrationId))
                .season(Optional.ofNullable(season))
                .id(Optional.ofNullable(id))
                .state(Optional.ofNullable(state))
                .email(Optional.ofNullable(email))
                .outstandingBalance(Optional.ofNullable(outstandingBalance))
                .isStaff(Optional.ofNullable(isStaff))
                .created(Optional.ofNullable(created))
                .parentUserId(Optional.ofNullable(parentUserId))
                .waiverAcceptedTimestamp(Optional.ofNullable(waiverAcceptedTimestamp))
                .firstName(Optional.ofNullable(firstName))
                .parentFirstName(Optional.ofNullable(parentFirstName))
                .groupName(Optional.ofNullable(groupName))
                .userProfileId(Optional.ofNullable(userProfileId))
                .userType(Optional.ofNullable(userType))
                .lastName(Optional.ofNullable(lastName))
                .role(Optional.ofNullable(role))
                .gender(Optional.ofNullable(gender))
                .city(Optional.ofNullable(city))
                .siteName(Optional.ofNullable(siteName))
                .parentPhone(Optional.ofNullable(parentPhone))
                .amountPaid(Optional.ofNullable(amountPaid))
                .registrationEndDate(Optional.ofNullable(registrationEndDate))
                .programState(Optional.ofNullable(programState))
                .programEndDate(Optional.ofNullable(programEndDate))
                .currentGradeLevel(Optional.ofNullable(currentGradeLevel))
                .lastPaymentDate(Optional.ofNullable(lastPaymentDate))
                .paymentStatus(Optional.ofNullable(paymentStatus))
                .paymentPlan(Optional.ofNullable((paymentPlan)))
                .programType(Optional.ofNullable(programType))
                .parentLastName(Optional.ofNullable(parentLastName))
                .address1(Optional.ofNullable(address1))
                .totalAmountDue(Optional.ofNullable(totalAmountDue))
                .userName(Optional.ofNullable(userName))
                .userId(Optional.ofNullable(userId))
                .birthDate(Optional.ofNullable(birthDate))
                .isCoCaptain(Optional.ofNullable(isCoCaptain))
                .sportId(Optional.ofNullable(sportId))
                .phone(Optional.ofNullable(phone))
                .currentClubTeamAffiliation(Optional.ofNullable(currentClubTeamAffiliation))
                .masterProgramName(Optional.ofNullable(masterProgramName))
                .generalTermsOfService(Optional.ofNullable(generalTermsOfService))
                .teamId(Optional.ofNullable(teamId))
                .currentSchoolAttending(Optional.ofNullable(currentSchoolAttending))
                .invoiceId(Optional.ofNullable(invoiceId))
                .programStartDate(Optional.ofNullable(programStartDate))
                .sport(Optional.ofNullable(sport))
                .programId(Optional.ofNullable(programId))
                .build();
    }    
}
