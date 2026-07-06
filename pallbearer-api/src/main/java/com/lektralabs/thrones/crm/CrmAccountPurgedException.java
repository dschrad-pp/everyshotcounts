package com.lektralabs.thrones.crm;

/**
 * The CRM answered 410 Gone: the account's 30-day grace period already ended and the CRM
 * purged it (tombstoned). Restore is permanently impossible; the API surfaces this to the
 * app as {@code 410 {code:"account_purged"}}.
 */
public class CrmAccountPurgedException extends Exception {

    public CrmAccountPurgedException(String email) {
        super("CRM account permanently purged for email: " + email);
    }
}
