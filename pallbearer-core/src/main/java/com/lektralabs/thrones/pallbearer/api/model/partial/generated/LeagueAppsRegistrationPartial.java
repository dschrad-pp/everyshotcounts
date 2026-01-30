package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsRegistrationRow;
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
public class LeagueAppsRegistrationPartial implements Serializable, EntityMethods {

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
    private Optional<Long> id;
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

    public LeagueAppsRegistrationRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  LeagueAppsRegistrationRow.builder()
                .zipCode(zipCode)
                .registrationStatus(registrationStatus)
                .parentEmail(parentEmail)
                .registrationStartDate(registrationStartDate)
                .groupId(groupId)
                .paymentPlanStatus(paymentPlanStatus)
                .masterProgramId(masterProgramId)
                .lastUpdated(lastUpdated)
                .price(price)
                .programName(programName)
                .registrationId(registrationId)
                .season(season)
                .id(id.get())
                .state(state)
                .email(email)
                .outstandingBalance(outstandingBalance)
                .isStaff(isStaff)
                .created(created)
                .parentUserId(parentUserId)
                .waiverAcceptedTimestamp(waiverAcceptedTimestamp)
                .firstName(firstName)
                .parentFirstName(parentFirstName)
                .groupName(groupName)
                .userProfileId(userProfileId)
                .userType(userType)
                .lastName(lastName)
                .role(role)
                .gender(gender)
                .city(city)
                .siteName(siteName)
                .parentPhone(parentPhone)
                .amountPaid(amountPaid)
                .registrationEndDate(registrationEndDate)
                .programState(programState)
                .programEndDate(programEndDate)
                .currentGradeLevel(currentGradeLevel)
                .lastPaymentDate(lastPaymentDate)
                .paymentStatus(paymentStatus)
                .paymentPlan(paymentPlan)
                .programType(programType)
                .parentLastName(parentLastName)
                .address1(address1)
                .totalAmountDue(totalAmountDue)
                .userName(userName)
                .userId(userId)
                .birthDate(birthDate)
                .isCoCaptain(isCoCaptain)
                .sportId(sportId)
                .phone(phone)
                .currentClubTeamAffiliation(currentClubTeamAffiliation)
                .masterProgramName(masterProgramName)
                .generalTermsOfService(generalTermsOfService)
                .teamId(teamId)
                .currentSchoolAttending(currentSchoolAttending)
                .invoiceId(invoiceId)
                .programStartDate(programStartDate)
                .sport(sport)
                .programId(programId)
                .build();
    }


}