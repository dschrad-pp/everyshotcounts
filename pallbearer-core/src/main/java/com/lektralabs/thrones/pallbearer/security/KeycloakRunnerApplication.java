package com.lektralabs.thrones.pallbearer.security;

import com.lektralabs.thrones.pallbearer.api.model.keycloak.OpenIdResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;

public class KeycloakRunnerApplication implements QuarkusApplication {
    @Inject
    KeycloakProvider keycloakProvider;

    private void testClientLogin() {
        System.out.println("Logging in with alice/alice");
        OpenIdResponse response = keycloakProvider.getUserAccessToken("alice", "alice");
        System.out.println("Response is " + response);
    }

    private void testAdminLogin() {
        System.out.println("Logging in with admin");
        OpenIdResponse response = keycloakProvider.getAdminAccessToken();
        System.out.println("Response is " + response);
    }

    private void testClientRegistration() throws Exception {
        System.out.println("Registering with carol/carol");
        RegisterUserPartial registerUserPartial = new RegisterUserPartial(
                "carol",
                "carol",
                "carol@lektralabs.com",
                "Carol",
                "Karrol",
                "(515) 555-1515",
                641588400000L,
                "PROVIDER",
                null
        );
        keycloakProvider.registerUser(registerUserPartial);
    }

    private void testFindUser() throws Exception {
        System.out.println("Registering with carol/carol");
        RegisterUserPartial registerUserPartial = new RegisterUserPartial(
                "carol",
                "carol",
                "carol@lektralabs.com",
                "Carol",
                "Karrol",
                "(515) 555-1515",
                641588400000L,
                "PROVIDER",
                null
        );
        System.out.println(keycloakProvider.findUser(registerUserPartial));
    }

    @Override
    public int run(String... args) throws Exception {
        System.out.println("KeycloakRunnerApplication::run started");

        testClientLogin();
        testAdminLogin();
        testClientRegistration();
        testFindUser();

        Quarkus.waitForExit();
        return 0;
    }

}
