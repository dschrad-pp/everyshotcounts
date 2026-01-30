package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.NotificationDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.NotificationBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class NotificationService extends NotificationBaseService {
    private static final Logger logger = Logger.getLogger(NotificationService.class);

    private NotificationDao notificationDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.notificationDao = jdbiProvider.getJdbi().onDemand(NotificationDao.class);
    }


}
