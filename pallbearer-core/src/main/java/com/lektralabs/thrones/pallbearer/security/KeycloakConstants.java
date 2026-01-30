package com.lektralabs.thrones.pallbearer.security;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public interface KeycloakConstants {

    String FORM_USERNAME = "username";
    String FORM_PASSWORD = "password";
    String FORM_GRANT_TYPE = "grant_type";
    String FORM_CLIENT_ID = "client_id";

    String LOGOUT_URL_FRAGMENT = "/protocol/openid-connect/logout";
    String AUTH_HEADER = "Authorization";
    String AUTH_BEARER = "Bearer ";

    String GRANT_TYPE = "password";

    String MASTER_REALM = "master";
    String MASTER_CLIENT = "admin-cli";
    String MASTER_SECRET = "c75da26e-a440-432d-8c19-cfe823e2e2f3";

    static final Config config = ConfigProvider.getConfig();

    public static final String KEYCLOAK_BASE_URL
            = config.getOptionalValue("keycloak.url", String.class).orElse("http://localhost:8080");

    String MASTER_URL = KEYCLOAK_BASE_URL + "/realms/master";

    String TOKEN_URL_FRAGMENT = "/protocol/openid-connect/token";
    String REALM_ROLES_BASE_URL = KEYCLOAK_BASE_URL + "/admin/realms/thrones_realm/roles";

    String ADMIN_USERNAME = "admin";
    String ADMIN_PASSWORD = "91GYV9rseDLnqTwz";

    String USER_URL = KEYCLOAK_BASE_URL + "/admin/realms/thrones_realm/users";
    String USER_REALM = "thrones_realm";

    String ROLE_URL = KEYCLOAK_BASE_URL + "/admin/realms/thrones_realm/users/%s/role-mappings/realm";
    String GROUP_URL = KEYCLOAK_BASE_URL + "/admin/realms/thrones_realm/users/%s/groups/%s";
    String PASSWORD_URL = KEYCLOAK_BASE_URL + "/admin/realms/thrones_realm/users/%s/reset-password";
    String USERNAME_URL = KEYCLOAK_BASE_URL + "/admin/realms/thrones_realm/users/%s";

}
