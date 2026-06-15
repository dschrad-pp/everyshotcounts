package com.lektralabs.thrones.pallbearer.api.auth;

/**
 * Stable, machine-readable error codes for the authentication endpoints.
 *
 * <p>Clients should branch on {@code error_code} (the enum {@link #name()}), never on the
 * human-readable message string, which is free to change for wording/localization. Each code
 * carries its canonical HTTP status and a default user-facing message.
 *
 * <p>Design notes:
 * <ul>
 *   <li>{@link #INVALID_CREDENTIALS} is intentionally returned for <em>both</em> a wrong password
 *       and an unknown user — identical status, code and message — to avoid user enumeration
 *       (OWASP Authentication guidance). A 401 from a <em>login</em> endpoint means "bad
 *       credentials", not "session expired"; clients must not treat it as a token-expiry logout.</li>
 *   <li>{@code 401} is reserved across the app for "you are not authenticated"; genuine token
 *       expiry on protected endpoints also uses 401, so clients disambiguate via {@code error_code}
 *       and/or the originating endpoint.</li>
 * </ul>
 */
public enum AuthErrorCode {

    /** Missing/empty/malformed username or password. */
    VALIDATION_ERROR(400, "Username and password are required"),

    /** Wrong password or unknown user — deliberately indistinguishable to the client. */
    INVALID_CREDENTIALS(401, "Incorrect email or password"),

    /** Credentials are valid in the CRM but the user has no local registration record yet. */
    REGISTRATION_INCOMPLETE(403, "No registration found. Please complete your registration."),

    /** Authenticated but the account has no paid/trial entitlement for this app. */
    PAYMENT_REQUIRED(403, "Payment required to access this app"),

    /** Too many failed attempts for this account or IP; see Retry-After header / retry_after_seconds. */
    TOO_MANY_ATTEMPTS(429, "Too many login attempts. Please wait before trying again."),

    /** An upstream dependency (CRM / identity provider) was unreachable or errored. */
    SERVICE_UNAVAILABLE(503, "Login is temporarily unavailable. Please try again shortly."),

    /** Unexpected server-side failure. */
    INTERNAL_ERROR(500, "Something went wrong. Please try again.");

    private final int httpStatus;
    private final String defaultMessage;

    AuthErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
