package com.lektralabs.thrones.pallbearer.api.account;

import com.lektralabs.thrones.crm.CrmAccountPurgedException;
import com.lektralabs.thrones.crm.CrmApiClient;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.CrmRegistrationRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.CrmRegistrationService;
import com.lektralabs.thrones.pallbearer.jdbi.service.DeviceTokenService;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The two invariants the app relies on:
 * <ul>
 *   <li><b>CRM-failure aborts:</b> if the CRM leg fails, nothing is committed locally —
 *       no flags, no session revocation, no token deletion — so a retry is always safe;</li>
 *   <li><b>Idempotency:</b> repeating a delete while already pending returns the existing
 *       purge date without touching the CRM or local state again.</li>
 * </ul>
 */
public class AccountDeletionServiceTest {

    private static final long NOW = 1_751_700_000_000L;
    private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID KEYCLOAK_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private CrmApiClient crmApiClient;
    private UserService userService;
    private CrmRegistrationService crmRegistrationService;
    private KeycloakProvider keycloakProvider;
    private DeviceTokenService deviceTokenService;

    private AccountDeletionService service;

    @BeforeEach
    void setUp() {
        crmApiClient = mock(CrmApiClient.class);
        userService = mock(UserService.class);
        crmRegistrationService = mock(CrmRegistrationService.class);
        keycloakProvider = mock(KeycloakProvider.class);
        deviceTokenService = mock(DeviceTokenService.class);

        service = new AccountDeletionService();
        service.crmApiClient = crmApiClient;
        service.userService = userService;
        service.crmRegistrationService = crmRegistrationService;
        service.keycloakProvider = keycloakProvider;
        service.deviceTokenService = deviceTokenService;
        service.clock = () -> NOW;
    }

    private UserRow user() {
        return UserRow.builder()
                .id(USER_ID)
                .username("athlete1")
                .email("athlete1@example.com")
                .keycloakId(KEYCLOAK_ID)
                .build();
    }

    private UserRow pendingUser() {
        UserRow user = user();
        user.setDeletionRequestedAt(NOW - 1000);
        user.setPurgeAfter(NOW - 1000 + AccountDeletionService.GRACE_PERIOD_MILLIS);
        return user;
    }

    private void givenCrmBackedUser() {
        when(crmRegistrationService.findByUsername("athlete1"))
                .thenReturn(Optional.of(mock(CrmRegistrationRow.class)));
    }

    private void givenLocalOnlyUser() {
        when(crmRegistrationService.findByUsername(anyString())).thenReturn(Optional.empty());
        when(crmRegistrationService.findByEmail(anyString())).thenReturn(Optional.empty());
    }

    // ── Idempotency ──────────────────────────────────────────────────────────

    @Test
    void requestDeletion_alreadyPending_returnsExistingPurgeDate_withoutTouchingAnything() throws Exception {
        UserRow pending = pendingUser();
        long existingPurgeAfter = pending.getPurgeAfter();

        AccountDeletionService.DeletionResult result = service.requestDeletion(pending);

        assertEquals(existingPurgeAfter, result.purgeAfter());
        verifyNoInteractions(crmApiClient);
        verify(userService, never()).updateDeletionState(any(), any(), any());
        verifyNoInteractions(keycloakProvider);
        verifyNoInteractions(deviceTokenService);
    }

    @Test
    void restore_notPending_succeedsWithoutClearingFlags() throws Exception {
        givenCrmBackedUser();
        when(crmApiClient.restoreAccount("athlete1@example.com")).thenReturn(Boolean.TRUE);

        AccountDeletionService.RestoreResult result = service.restore(user());

        assertTrue(result.subscriptionRestored());
        verify(userService, never()).updateDeletionState(any(), any(), any());
    }

    // ── CRM failure aborts: nothing committed locally ────────────────────────

    @Test
    void requestDeletion_crmFailure_commitsNothingLocally() throws Exception {
        givenCrmBackedUser();
        when(crmApiClient.requestAccountDeletion("athlete1@example.com"))
                .thenThrow(new IOException("CRM down"));

        assertThrows(IOException.class, () -> service.requestDeletion(user()));

        verify(userService, never()).updateDeletionState(any(), any(), any());
        verifyNoInteractions(keycloakProvider);
        verifyNoInteractions(deviceTokenService);
    }

    @Test
    void restore_crmFailure_keepsLocalFlags() throws Exception {
        givenCrmBackedUser();
        when(crmApiClient.restoreAccount("athlete1@example.com"))
                .thenThrow(new IOException("CRM down"));

        assertThrows(IOException.class, () -> service.restore(pendingUser()));

        verify(userService, never()).updateDeletionState(any(), any(), any());
    }

    @Test
    void restore_crmPurged_propagatesAndKeepsLocalFlags() throws Exception {
        givenCrmBackedUser();
        when(crmApiClient.restoreAccount("athlete1@example.com"))
                .thenThrow(new CrmAccountPurgedException("athlete1@example.com"));

        assertThrows(CrmAccountPurgedException.class, () -> service.restore(pendingUser()));

        verify(userService, never()).updateDeletionState(any(), any(), any());
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    void requestDeletion_crmBacked_callsCrmBeforeCommittingFlags() throws Exception {
        givenCrmBackedUser();
        when(crmApiClient.requestAccountDeletion("athlete1@example.com")).thenReturn("2026-08-04T00:00:00Z");

        AccountDeletionService.DeletionResult result = service.requestDeletion(user());

        assertEquals(NOW + AccountDeletionService.GRACE_PERIOD_MILLIS, result.purgeAfter());
        var order = inOrder(crmApiClient, userService, keycloakProvider, deviceTokenService);
        order.verify(crmApiClient).requestAccountDeletion("athlete1@example.com");
        order.verify(userService).updateDeletionState(USER_ID, NOW, NOW + AccountDeletionService.GRACE_PERIOD_MILLIS);
        order.verify(keycloakProvider).logoutAllSessions(KEYCLOAK_ID);
        order.verify(deviceTokenService).deleteAllByUserId(USER_ID);
    }

    @Test
    void requestDeletion_localOnlyCoach_skipsCrmLeg() throws Exception {
        givenLocalOnlyUser();

        AccountDeletionService.DeletionResult result = service.requestDeletion(user());

        assertEquals(NOW + AccountDeletionService.GRACE_PERIOD_MILLIS, result.purgeAfter());
        verifyNoInteractions(crmApiClient);
        verify(userService).updateDeletionState(USER_ID, NOW, NOW + AccountDeletionService.GRACE_PERIOD_MILLIS);
    }

    @Test
    void requestDeletion_keycloakRevocationFailure_doesNotFailTheRequest() throws Exception {
        givenLocalOnlyUser();
        org.mockito.Mockito.doThrow(new IllegalArgumentException("keycloak down"))
                .when(keycloakProvider).logoutAllSessions(KEYCLOAK_ID);

        AccountDeletionService.DeletionResult result = service.requestDeletion(user());

        // Flags committed, tokens still cleaned up; the pending-deletion filter is the backstop.
        assertEquals(NOW + AccountDeletionService.GRACE_PERIOD_MILLIS, result.purgeAfter());
        verify(userService).updateDeletionState(USER_ID, NOW, NOW + AccountDeletionService.GRACE_PERIOD_MILLIS);
        verify(deviceTokenService).deleteAllByUserId(USER_ID);
    }

    @Test
    void restore_pending_clearsFlagsAfterCrmSuccess() throws Exception {
        givenCrmBackedUser();
        when(crmApiClient.restoreAccount("athlete1@example.com")).thenReturn(Boolean.FALSE);

        AccountDeletionService.RestoreResult result = service.restore(pendingUser());

        // Canceled trials are not resurrected — the app prompts to re-subscribe.
        assertFalse(result.subscriptionRestored());
        var order = inOrder(crmApiClient, userService);
        order.verify(crmApiClient).restoreAccount("athlete1@example.com");
        order.verify(userService).updateDeletionState(USER_ID, null, null);
    }

    @Test
    void restore_localOnlyCoach_skipsCrmAndClearsFlags() throws Exception {
        givenLocalOnlyUser();

        AccountDeletionService.RestoreResult result = service.restore(pendingUser());

        assertTrue(result.subscriptionRestored());
        verifyNoInteractions(crmApiClient);
        verify(userService).updateDeletionState(USER_ID, null, null);
    }
}
