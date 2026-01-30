package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.DrillGroupDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.DrillGroupBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class DrillGroupService extends DrillGroupBaseService {
    private static final Logger logger = Logger.getLogger(DrillGroupService.class);

    private DrillGroupDao drillGroupDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.drillGroupDao = jdbiProvider.getJdbi().onDemand(DrillGroupDao.class);
    }


}
