package com.lektralabs.thrones.crm;

/**
 * Outcome of {@link CrmApiClient#validateGoogleCredential(String)}.
 *
 * <p>Unlike password validation — which only has to answer yes/no — Google sign-in needs a
 * four-way answer, because the app shows different guidance in each case ("sign up on the web",
 * "you use a password", "contact support", "try again"). And unlike the password path, the
 * backend does not know <em>who the user is</em> until the CRM replies: the request carries only
 * an opaque ID token. So this object carries the identity too.
 *
 * <p>Deliberately a plain class and not a {@code record}: {@code jandex-maven-plugin} is pinned
 * at 1.2.3 in {@code pallbearer-reactor/pom.xml} and throws {@code EOFException} on record class
 * files, failing the build after compilation.
 */
public class CrmGoogleResult {

    private final boolean valid;

    /** Null on success; otherwise NO_ACCOUNT / NOT_LINKED / ACCOUNT_INACTIVE / INVALID_GOOGLE_TOKEN. */
    private final String code;

    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private final String role;

    private final boolean accountPendingDeletion;
    private final Long purgeAfter;

    private CrmGoogleResult(boolean valid, String code,
                            String username, String email,
                            String firstName, String lastName,
                            String phoneNumber, String role,
                            boolean accountPendingDeletion, Long purgeAfter) {
        this.valid = valid;
        this.code = code;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.accountPendingDeletion = accountPendingDeletion;
        this.purgeAfter = purgeAfter;
    }

    /** Identity confirmed by the CRM. */
    public static CrmGoogleResult success(String username, String email,
                                          String firstName, String lastName,
                                          String phoneNumber, String role,
                                          boolean accountPendingDeletion, Long purgeAfter) {
        return new CrmGoogleResult(true, null, username, email, firstName, lastName,
                phoneNumber, role, accountPendingDeletion, purgeAfter);
    }

    /**
     * Identity rejected. {@code email} is populated for NO_ACCOUNT and NOT_LINKED (the CRM
     * returns it so the app can name the address in its message); null otherwise.
     */
    public static CrmGoogleResult failure(String code, String email) {
        return new CrmGoogleResult(false, code, null, email, null, null, null, null, false, null);
    }

    public boolean isValid() {
        return valid;
    }

    public String getCode() {
        return code;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getRole() {
        return role;
    }

    public boolean isAccountPendingDeletion() {
        return accountPendingDeletion;
    }

    public Long getPurgeAfter() {
        return purgeAfter;
    }
}
