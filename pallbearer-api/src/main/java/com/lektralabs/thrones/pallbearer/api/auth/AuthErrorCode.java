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

    /** Missing/empty/malformed email or password. */
    VALIDATION_ERROR(400, "Email and password are required"),

    /** Wrong password or unknown user — deliberately indistinguishable to the client. */
    INVALID_CREDENTIALS(401, "Incorrect email or password"),

    /** Credentials are valid in the CRM but the user has no local registration record yet. */
    REGISTRATION_INCOMPLETE(403, "No registration found. Please complete your registration."),

    /** Authenticated but the account has no paid/trial entitlement for this app. */
    PAYMENT_REQUIRED(403, "Payment required to access this app"),

    /**
     * Google sign-in only. The Google identity is genuine but no ESC account exists for it.
     * Sign-up lives on the web CRM, so the app deep-links there rather than showing a dead end.
     * Not a failed credential — never counted against the brute-force limiter.
     */
    NO_ACCOUNT(404, "No ESC account found for this Google account. Sign up at everyshotcountsapp.com"),

    /**
     * Google sign-in only. An ESC account has this email but Google is not connected to it.
     * Deliberately NOT auto-linked: the web flow requires the account's password before
     * connecting Google, because a wrong link hands over someone's profile, payment state and —
     * for athletes — a minor's date of birth.
     */
    NOT_LINKED(409, "This email uses a password. Sign in with your password, or connect Google on the web."),

    /** The account exists and is linked, but has been deactivated in the CRM. */
    ACCOUNT_INACTIVE(403, "This account is inactive. Please contact support."),

    /**
     * Entitlement lapsed: {@code payment_status} still reads as paid/trial but
     * {@code subscription_end_date} is in the past. Distinct from {@link #PAYMENT_REQUIRED} so
     * the app can say "renew" rather than "pay".
     *
     * <p>Currently returned ONLY by {@code /api/sso/google-login}, and only when
     * {@code auth.subscription.enddate.enforced} is true. Before returning it from
     * {@code /api/sso/crm-login}, confirm the shipped iOS build degrades gracefully on an
     * unknown {@code error_code} — see todo.md, 07/28/26.
     */
    SUBSCRIPTION_EXPIRED(403, "Your subscription has ended. Renew to keep using the app."),

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
