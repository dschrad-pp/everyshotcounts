package com.lektralabs.attic.thrones.pallbearer;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import io.quarkus.cache.CacheKey;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

@Alternative
@Priority(1)
@Singleton
public class MockUserService extends UserService {

    public CurrentUser getCurrentUser(@CacheKey String username) {
        return super.getCurrentUser("kbrumer");
    }

}
