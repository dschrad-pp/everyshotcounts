package com.lektralabs.thrones.pallbearer.jdbi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Just the account-deletion grace fields for one user.
 * <p>
 * {@code AccountPendingDeletionFilter} runs on EVERY authenticated request and
 * only ever reads these two timestamps. Loading a whole {@link UserRow} to do
 * it meant every API call also dragged {@code avatar_byte_array} — a BYTEA
 * column — out of the database and across the wire, for nothing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletionStateRow {

    /** Epoch millis when deletion was requested; null when the account is active. */
    private Long deletionRequestedAt;

    /** Epoch millis when the grace window expires; null when not pending deletion. */
    private Long purgeAfter;
}
