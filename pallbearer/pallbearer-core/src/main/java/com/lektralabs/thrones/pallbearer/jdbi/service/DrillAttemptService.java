package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillAttemptDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.DrillAttemptBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten
@ApplicationScoped
public class DrillAttemptService extends DrillAttemptBaseService {

    private static final Logger logger = Logger.getLogger(DrillAttemptService.class);

    private DrillAttemptDao drillAttemptDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.drillAttemptDao = jdbiProvider.getJdbi().onDemand(DrillAttemptDao.class);
    }

}
