package com.lektralabs.thrones.pallbearer.jdbi.exception;

import java.util.Map;

public class RegistrationException extends RuntimeException {

    public static final String EMAIL_MATCH_FAILURE = "EMAIL_MATCH_FAILURE";

    public static final String INCORRECT_REGISTRATION_CODE = "INCORRECT_REGISTRATION_CODE";

    public static final String USER_ALREADY_REGISTERED = "USER_ALREADY_REGISTERED";

    public static final String USERNAME_COLLISION = "USERNAME_COLLISION";

    public static final String EMAIL_COLLISION = "EMAIL_COLLISION";

    public static final String PASSWORDS_DONT_MATCH = "PASSWORDS_DONT_MATCH";

    private static final Map<String, String> USER_FRIENDLY_MESSAGES = Map.of(
        EMAIL_MATCH_FAILURE, "No account found with this email address. Please check your email and try again.",
        INCORRECT_REGISTRATION_CODE, "Invalid registration code. Please check your code and try again.",
        USER_ALREADY_REGISTERED, "This account has already been activated. Please log in instead.",
        USERNAME_COLLISION, "This username is already in use. Please choose a different username.",
        EMAIL_COLLISION, "This email address is already in use by another account. Please use a different email address.",
        PASSWORDS_DONT_MATCH, "Passwords do not match. Please make sure both password fields are the same."
    );

    /**
     * Checks if the exception message contains a known error code and returns user-friendly message
     */
    private String getUserFriendlyMessageForMessage(String message) {
        if (message == null) {
            return "An error occurred during registration. Please try again.";
        }
        
        // Check for exact matches first
        if (USER_FRIENDLY_MESSAGES.containsKey(message)) {
            return USER_FRIENDLY_MESSAGES.get(message);
        }
        
        // Check if message contains any of the error codes
        for (String code : USER_FRIENDLY_MESSAGES.keySet()) {
            if (message.contains(code)) {
                return USER_FRIENDLY_MESSAGES.get(code);
            }
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Check for Keycloak errors - extract from cause if available
        if (lowerMessage.contains("keycloak activation failed") || 
            lowerMessage.contains("keycloak registration failed")) {
            // Try to extract the actual error from the cause
            Throwable cause = getCause();
            if (cause != null && cause.getMessage() != null) {
                String causeMessage = cause.getMessage().toLowerCase();
                // Check if cause mentions email or username already in use
                if (causeMessage.contains("email") && (causeMessage.contains("exists") || 
                    causeMessage.contains("already") || causeMessage.contains("in use") ||
                    causeMessage.contains("user exists with same email"))) {
                    return "This email address is already in use by another account. Please use a different email address.";
                }
                if (causeMessage.contains("username") && (causeMessage.contains("exists") || 
                    causeMessage.contains("already") || causeMessage.contains("in use"))) {
                    return "This username is already in use. Please choose a different username.";
                }
            }
            return "Account activation failed. Please check your information and try again. If the problem persists, contact support.";
        }
        
        // Check for common Keycloak errors in the message itself
        if (lowerMessage.contains("user exists with same email") || 
            (lowerMessage.contains("email") && lowerMessage.contains("already in use"))) {
            return "This email address is already in use by another account. Please use a different email address.";
        }
        
        if (lowerMessage.contains("username") && (lowerMessage.contains("exists") || 
            lowerMessage.contains("already") || lowerMessage.contains("in use"))) {
            return "This username is already in use. Please choose a different username.";
        }
        
        if (lowerMessage.contains("email") && (lowerMessage.contains("exists") || 
            lowerMessage.contains("already") || lowerMessage.contains("in use"))) {
            return "This email address is already in use by another account. Please use a different email address.";
        }
        
        // Fallback to generic message
        return "An error occurred during registration. Please try again.";
    }

    public RegistrationException() {
    }

    public RegistrationException(String message) {
        super(message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns a user-friendly error message for the exception code
     */
    public String getUserFriendlyMessage() {
        return getUserFriendlyMessageForMessage(getMessage());
    }
}
