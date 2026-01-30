package com.lektralabs.attic.thrones.pallbearer.security;

import com.lektralabs.thrones.pallbearer.api.model.keycloak.OpenIdResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

public class KeycloakTestRunner {

    public static void main(String[] args) {
        Quarkus.run(KeycloakTestApp.class);
    }

    public static class KeycloakTestApp implements QuarkusApplication {
        @Inject
        KeycloakProvider keycloakProvider;

        @Override
        public int run(String... args) throws Exception {
            // getAdminToken();
            setRole4();
            System.out.println("DONE!");
            Quarkus.waitForExit();
            return 0;
        }

        private void getAdminToken() {
            OpenIdResponse response = keycloakProvider.getAdminAccessToken();
            System.out.println("response = [%s]".formatted(response));
        }

        private void setRole1() {
            RealmResource realmResource;
            try (Keycloak keycloak = keycloakProvider.keycloakFactory()) {
                realmResource = keycloak.realm("thrones_realm");
                UsersResource usersResource = realmResource.users();

                UserResource eveUserResource = usersResource.get("96d370a6-5cfe-4a6a-9b76-4249482361cb");
                RoleRepresentation coachRepresentation = realmResource.roles().get("COACH").toRepresentation();

                eveUserResource.roles().realmLevel().add(Arrays.asList(coachRepresentation));
            }
        }

        private void setRole2() {
            RealmResource realmResource;
            try (Keycloak keycloak = keycloakProvider.keycloakFactory()) {
                String userId = "96d370a6-5cfe-4a6a-9b76-4249482361cb";
                String role = "COACH";
                String realm = "thrones_realm";
                String clientId = "thrones_client";
                UsersResource usersResource = keycloak.realm(realm).users();
                UserResource userResource = usersResource.get(userId);

                //getting client
                ClientRepresentation clientRepresentation = keycloak.realm(realm).clients().findAll().stream().filter(client -> client.getClientId().equals(clientId)).collect(Collectors.toList()).get(0);
                ClientResource clientResource = keycloak.realm(realm).clients().get(clientRepresentation.getId());
                //getting role
                RoleRepresentation roleRepresentation = clientResource.roles().list().stream().filter(element -> element.getName().equals(role)).collect(Collectors.toList()).get(0);
                //assigning to user
                userResource.roles().clientLevel(clientRepresentation.getId()).add(Collections.singletonList(roleRepresentation));
            }
        }

        private void setRole3() {
            try (Keycloak keycloak = keycloakProvider.keycloakFactory()) {
                UUID userId = UUID.fromString("96d370a6-5cfe-4a6a-9b76-4249482361cb");
                RegisterUserPartial registerUserPartial = new RegisterUserPartial(
                        "eve",
                        "Password4u",
                        "eve@lektralabs.com",
                        "Eve",
                        "Evans",
                        "867-5309",
                        0L,
                        "COACH"
                );
                keycloakProvider.associateRoleWithUser(userId, registerUserPartial);

            }
        }

        private void setRole4() {
            try (Keycloak keycloak = keycloakProvider.keycloakFactory()) {
                UUID userId = UUID.fromString("96d370a6-5cfe-4a6a-9b76-4249482361cb");
                RegisterUserPartial registerUserPartial = new RegisterUserPartial(
                        "eve",
                        "Password4u",
                        "eve@lektralabs.com",
                        "Eve",
                        "Evans",
                        "867-5309",
                        0L,
                        "COACH"
                );
                keycloakProvider.associateGroupWithUser(userId, registerUserPartial);

            }
        }

    }
}

