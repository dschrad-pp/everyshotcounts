package com.lektralabs.thrones.pallbearer.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code POST /api/sso/google-login}.
 *
 * <p>{@code LoginUser} does not fit: it is an immutable username/password pair, and a Google
 * sign-in carries neither — only the raw ID token issued by the iOS Google Sign-In SDK.
 *
 * <p>The token is passed through to the CRM verbatim. Do not decode, re-sign or unwrap it; the
 * CRM verifies the signature against Google's keys and checks the audience, and any modification
 * fails that check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    /**
     * The Google ID token (a JWT). This is a bearer credential AND its payload carries the
     * user's email and name as readable claims — never log it. See the note in
     * {@code CrmApiClient.validateGoogleCredential}.
     */
    private String credential;
}
