package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.OrganizationDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.OrganizationBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class OrganizationService extends OrganizationBaseService {
    private static final Logger logger = Logger.getLogger(OrganizationService.class);

    private OrganizationDao organizationDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.organizationDao = jdbiProvider.getJdbi().onDemand(OrganizationDao.class);
    }


}
