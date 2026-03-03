package com.lektralabs.thrones.leagueapps;

import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.EscLeagueAppsMemberActionPartial;
import com.lektralabs.thrones.pallbearer.common.UserPropertyConstants;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.jdbi.exception.RegistrationException;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.EscActionItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.RegistrationItem;
import com.lektralabs.thrones.pallbearer.jdbi.service.EscLeagueAppsMemberActionService;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserPropertyService;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserGroupPropertyService;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@ApplicationScoped
public class LeagueAppsActions implements UserPropertyConstants {

    private static final Logger logger = Logger.getLogger(LeagueAppsActions.class);

    private final Random random = new Random();

    @Inject
    UserService userService;

    @Inject
    UserPropertyService userPropertyService;

    @Inject
    UserGroupPropertyService userGroupPropertyService;

    @Inject
    EscLeagueAppsMemberActionService escLeagueAppsMemberActionService;

    protected void processActionItem(EscActionItem actionItem) {
        System.out.println("=========================================================================== \n");
        System.out.println("processActionItem Line 43: " + actionItem);
        System.out.println("=========================================================================== \n");
        EscActionEnum action = getAction(actionItem);
        switch (action) {
            case NEW_REGISTRATION:
                autoRegisterUser(actionItem);
                break;
            case STOP_PAYMENT:
                unregisterUser(actionItem);
                break;
            case NO_REGISTRATION_NO_PAYMENT:
            case REGISTERED:
                logNoop(action, actionItem);
                break;
        }
        setActionCodeToCompleted(actionItem);
    }

    /**
     * This is used when we detect a new user from the ESC application, and we
     * automatically register them.
     *
     * @param actionItem
     */
    protected void autoRegisterUser(EscActionItem actionItem) {
        EscLeagueAppsMemberActionPartial partial = actionItem.getElama();
        System.out.println("=========================================================================== \n");
        logger.info("EscLeagueAppsMemberActionPartial: " + partial.getPaymentStatus());
        logger.info("EscLeagueAppsMemberActionPartial email: " + partial.getEmail());
        System.out.println("=========================================================================== \n");

        // Prepare user property map
        Map<String, String> userPropertyMap = new java.util.HashMap<>();

        // Generate six digit code for registration
        String sixDigitCode = createRegistrationKey();
        userPropertyMap.put(UserPropertyConstants.USER_REGISTRATION_SIX_DIGIT_CODE_KEY, sixDigitCode);

        // Normalize payment status and add to properties if present
        partial.getPaymentStatus().ifPresent(paymentStatus -> {
            userPropertyMap.put(UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_KEY,
                    normalizePaymentStatus(paymentStatus));
        });

        // Check if a user with the given ESC username already exists
        Optional<UserRow> maybeExistingUser = userService.findByEmail(partial.getEmail().get());
        // System.out.println("===========================================================================
        // \n");
        // System.out.println("maybeExistingUser: " + maybeExistingUser);
        // System.out.println("===========================================================================
        // \n");
        if (maybeExistingUser.isPresent()) {
            UserRow existingUser = maybeExistingUser.get();
            // System.out.println("===========================================================================
            // \n");
            // System.out.println("existingUser: " + existingUser);
            // System.out.println("===========================================================================
            // \n");
            // Always update their payment status property
            userPropertyService.addProperties(existingUser.getId(), userPropertyMap);

            // If user has a Keycloak ID, consider them fully registered and skip new
            // registration
            if (existingUser.getKeycloakId() != null) {
                // System.out.println("===========================================================================
                // \n");
                // logger.info("User already registered with username: " +
                // existingUser.getUsername());
                // System.out.println("User already registered with email: " +
                // existingUser.getEmail());
                // System.out.println("===========================================================================
                // \n");
                return;

            }

            // Otherwise continue to create a new user below (re-register inactive users if
            // desired)
        } else {
            // Build the new user registration object
            // Username is stored in lowercase for consistency
            String username = partial.getUsernameLa() != null ? partial.getUsernameLa().toLowerCase() : null;
            RegisterUserPartial registerUserPartial = RegisterUserPartial.builder()
                    .username(username)
                    .password(UUID.randomUUID().toString())
                    .email(partial.getEmail().orElse("<EMAIL>"))
                    .firstName(partial.getFirstName().orElse("<FIRST NAME>"))
                    .lastName(partial.getLastName().orElse("<LAST NAME>"))
                    .phoneNumber(partial.getMobilePhone().orElse("<MOBILE PHONE>"))
                    .birthDate(partial.getBirthDate().orElse(0L))
                    .role("ATHLETE")
                    .build();

            // Register the new user
            UserRow userRow = userService.registerUser(registerUserPartial, false);

            // Save user properties for the new user
            userPropertyService.addProperties(userRow.getId(), userPropertyMap);

            // Send new registration email to the user
            try {
                userService.newRegistrationEmail(
                        partial.getEmail().orElse("support@everyshotcounts.com"),
                        sixDigitCode);
            } catch (Exception e) {
                logger.error("Could not send email to user [%s]".formatted(registerUserPartial), e);
            }
        }
    }

    protected String normalizePaymentStatus(String paymentStatus) {
        if (paymentStatus == null) {
            return "";
        }
        switch (paymentStatus.toUpperCase()) {
            case "PAID":
                return UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_PAID;
            case "TRIAL":
            case "TRAIL":
                return UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_TRIAL;
            default:
                return paymentStatus.toUpperCase();
        }
    }

    /**
     * Called when a user tries to register with a six digit code
     *
     * @param registrationItem
     */
    protected void selfRegisterUser(RegistrationItem registrationItem) {
        if (!registrationItem.getPassword().equals(registrationItem.getConfirmPassword())) {
            throw new RegistrationException(RegistrationException.PASSWORDS_DONT_MATCH);
        }

        Optional<UserRow> optUserRow = userService.findByRegistrationCode(registrationItem.getSixDigitCode());
        if (!optUserRow.isPresent()) {
            throw new RegistrationException(RegistrationException.INCORRECT_REGISTRATION_CODE);
        }
        UserRow userRow = optUserRow.get();
        // Username is stored in lowercase for consistency
        String username = registrationItem.getUsername() != null ? registrationItem.getUsername().toLowerCase() : null;
        RegisterUserPartial registerUserPartial = RegisterUserPartial.builder()
                .username(username)
                .password(registrationItem.getPassword())
                .email(registrationItem.getEmail())
                .role("ATHLETE")
                .build();
        userService.activateUserByCode(registerUserPartial, registrationItem.getSixDigitCode());

        Map<String, String> userPropertyMap = Map.ofEntries(
                Map.entry(USER_REGISTRATION_STATE_KEY, USER_REGISTRATION_STATE_REGISTERED),
                Map.entry(USER_REGISTRATION_PAYMENT_STATE_KEY, USER_REGISTRATION_PAYMENT_STATE_PAID),
                Map.entry(USER_REGISTRATION_DATE_KEY, DateTimeUtils.now().toString()),
                Map.entry(USER_DRILL_GROUP_KEY, registrationItem.getSkillLevel()),
                Map.entry(USER_DRILL_GROUP_LEVEL_KEY, "1.0"),
                Map.entry(USER_TERMS_AND_CONDITIONS_ACCEPT, registrationItem.getAgeAcknowledgement()),
                Map.entry(USER_METRIC_DRILL_GROUP_COMPLETION_PERCENT, "0"),
                Map.entry(USER_METRIC_DRILL_LEVEL_COMPLETION_PERCENT, "0"));
        userPropertyService.addProperties(userRow.getId(), userPropertyMap);
    }

    protected int unregisterUser(EscActionItem actionItem) {
        if (actionItem.getUser() == null || actionItem.getUser().getUserId() == null) {
            logger.warn("No user or userId found for actionItem [%s]".formatted(actionItem));
            return -1;
        }

        return actionItem.getUser().getUserId().flatMap(userId -> userPropertyService
                .findByKey(userId, UserPropertyConstants.USER_REGISTRATION_STATE_KEY).map(userPropertyRow -> {
                    return userPropertyService.update(
                            userPropertyRow.toBuilder()
                                    .propertyValue(UserPropertyConstants.USER_REGISTRATION_STATE_UNREGISTERED)
                                    .build());
                })).orElseGet(() -> {
                    logger.warn("Could not update user to unregistered [%s]".formatted(actionItem));
                    return -1;
                });
    }

    protected void logNoop(EscActionEnum action, EscActionItem actionItem) {
        logger.warn("No action taken [%s] for item [%s]".formatted(action, actionItem));
    }

    protected int setActionCodeToCompleted(EscActionItem actionItem) {
        return escLeagueAppsMemberActionService.update(
                actionItem.getElama().toBuilder().actionCode("COMPLETED").build());
    }

    protected EscActionEnum getAction(EscActionItem actionItem) {
        if (isNewUser(actionItem)) {
            System.out.println("=========================================================================== \n");
            System.out.println("isNewUser Line 223: " + isNewUser(actionItem));
            System.out.println("=========================================================================== \n");
            if (isPayment(actionItem)) {
                return EscActionEnum.NEW_REGISTRATION;
            } else {
                System.out.println("=========================================================================== \n");
                System.out.println("NO_REGISTRATION_NO_PAYMENT: " + EscActionEnum.NO_REGISTRATION_NO_PAYMENT);
                System.out.println("=========================================================================== \n");
                return EscActionEnum.NO_REGISTRATION_NO_PAYMENT;
            }
        } else {
            System.out.println("=========================================================================== \n");
            System.out.println("isRegistered Line 235: else statement");
            System.out.println("=========================================================================== \n");
            if (isPayment(actionItem)) {
                System.out.println("=========================================================================== \n");
                System.out.println("REGISTERED Line 239: " + EscActionEnum.REGISTERED);
                System.out.println("=========================================================================== \n");
                return EscActionEnum.REGISTERED;
            } else {
                System.out.println("=========================================================================== \n");
                System.out.println("STOP_PAYMENT Line 244: " + EscActionEnum.STOP_PAYMENT);
                System.out.println("=========================================================================== \n");
                return EscActionEnum.STOP_PAYMENT;
            }
        }
    }

    private boolean isNewUser(EscActionItem actionItem) {
        System.out.println("=========================================================================== \n");
        System.out.println("isNewUser Line 253: " + actionItem.getUser());
        System.out.println("=========================================================================== \n");
        return actionItem.getUser() == null
                || actionItem.getUser().getUserId() == null
                || actionItem.getUser().getUserId().isEmpty()
                || "UNREGISTERED".equals(actionItem.getElama().getActionCode());
    }

    private boolean isPayment(EscActionItem actionItem) {
        if (actionItem.getElama() != null) {
            Optional<String> paymentStatus = actionItem.getElama().getPaymentStatus();
            return contains(paymentStatus, "PAID") || contains(paymentStatus, "PARTIAL")
                    || contains(paymentStatus, "TRIAL");
        } else {
            return false;
        }
    }

    public static <T> boolean contains(Optional<T> input, T value) {
        return input.map(value::equals).orElseGet(() -> false);
    }

    public String createRegistrationKey() {
        int number = random.nextInt(999999);
        return String.format("%06d", number);
    }

}
