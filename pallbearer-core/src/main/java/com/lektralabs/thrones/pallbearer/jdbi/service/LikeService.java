package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.LikeDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.LikeBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class LikeService extends LikeBaseService {
    private static final Logger logger = Logger.getLogger(LikeService.class);

    private LikeDao likeDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.likeDao = jdbiProvider.getJdbi().onDemand(LikeDao.class);
    }


}
