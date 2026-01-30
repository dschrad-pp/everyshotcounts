package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SystemPropertiesPartial;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.SystemPropertiesDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SystemPropertiesRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.SystemPropertiesBaseService;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class SystemPropertiesService extends SystemPropertiesBaseService {
    private static final Logger logger = Logger.getLogger(SystemPropertiesService.class);

    private SystemPropertiesDao systemPropertiesDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.systemPropertiesDao = jdbiProvider.getJdbi().onDemand(SystemPropertiesDao.class);
    }


}
