package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.SocialMediaDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.SocialMediaBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class SocialMediaService extends SocialMediaBaseService {
    private static final Logger logger = Logger.getLogger(SocialMediaService.class);

    private SocialMediaDao socialMediaDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.socialMediaDao = jdbiProvider.getJdbi().onDemand(SocialMediaDao.class);
    }


}
