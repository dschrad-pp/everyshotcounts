package com.lektralabs.thrones.pallbearer.api.model.display;

import io.quarkus.security.identity.SecurityIdentity;

public class UserDisplay {

    private final String userName;

    public UserDisplay(SecurityIdentity securityContext) {
        this.userName = securityContext.getPrincipal().getName();
    }

    public String getUserName() {
        return userName;
    }
}
