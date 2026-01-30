package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.SportChallengeTypeDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.SportChallengeTypeBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class SportChallengeTypeService extends SportChallengeTypeBaseService {
    private static final Logger logger = Logger.getLogger(SportChallengeTypeService.class);

    private SportChallengeTypeDao sportChallengeTypeDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.sportChallengeTypeDao = jdbiProvider.getJdbi().onDemand(SportChallengeTypeDao.class);
    }


}
