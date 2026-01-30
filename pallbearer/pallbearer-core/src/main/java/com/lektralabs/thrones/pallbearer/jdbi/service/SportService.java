package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.SportDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.SportBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class SportService extends SportBaseService {
    private static final Logger logger = Logger.getLogger(SportService.class);

    private SportDao sportDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.sportDao = jdbiProvider.getJdbi().onDemand(SportDao.class);
    }


}
