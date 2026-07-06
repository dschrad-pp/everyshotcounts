package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.DeviceTokenDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.DeviceTokenRow;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DeviceTokenService {

    private static final Logger logger = Logger.getLogger(DeviceTokenService.class);

    @Inject
    JdbiProvider jdbiProvider;

    private DeviceTokenDao deviceTokenDao;

    @PostConstruct
    public void init() {
        this.deviceTokenDao = jdbiProvider.getJdbi().onDemand(DeviceTokenDao.class);
    }

    public void upsert(UUID userId, String token, String platform) {
        long now = System.currentTimeMillis();
        DeviceTokenRow row = DeviceTokenRow.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token(token)
                .platform(platform)
                .createdAt(now)
                .updatedAt(now)
                .build();
        deviceTokenDao.upsert(row);
        logger.infof("Upserted device token for userId=%s platform=%s", userId, platform);
    }

    public Optional<DeviceTokenRow> findByUserId(UUID userId, String platform) {
        return deviceTokenDao.findByUserId(userId, platform);
    }

    public void deleteByUserId(UUID userId, String platform) {
        deviceTokenDao.deleteByUserId(userId, platform);
    }

    /** Removes every platform's push token for the user (account deletion / purge). */
    public int deleteAllByUserId(UUID userId) {
        int deleted = deviceTokenDao.deleteAllByUserId(userId);
        logger.infof("Deleted %d device token(s) for userId=%s", deleted, userId);
        return deleted;
    }
}
