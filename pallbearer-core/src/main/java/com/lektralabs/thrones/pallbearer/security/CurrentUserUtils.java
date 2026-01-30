package com.lektralabs.thrones.pallbearer.security;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.CurrentUserDao;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class CurrentUserUtils {

    @Inject
    SecurityIdentity keycloakSecurityContext;

    @Inject
    JdbiProvider jdbiProvider;

    private CurrentUserDao currentUserDao;

    @PostConstruct
    public void init() {
        this.currentUserDao = jdbiProvider.getJdbi().onDemand(CurrentUserDao.class);
    }

    public UUID getCurrentUserId() {
        String username = keycloakSecurityContext.getPrincipal().getName();
        if (username == null) {
            throw new RuntimeException("Username cannot be null");
        }
        // SQL query uses case-insensitive comparison (lower()), so we lowercase for consistency
        String lowercasedUsername = username.toLowerCase();
        CurrentUser currentUser = currentUserDao.findByName(lowercasedUsername);
        
        if (currentUser == null) {
            throw new RuntimeException("User not found for username: " + username);
        }

        return currentUser.getId();
    }

}
