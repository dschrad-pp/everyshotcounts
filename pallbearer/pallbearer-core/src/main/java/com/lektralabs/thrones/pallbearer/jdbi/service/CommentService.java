package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.CommentDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.CommentBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class CommentService extends CommentBaseService {
    private static final Logger logger = Logger.getLogger(CommentService.class);

    private CommentDao commentDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.commentDao = jdbiProvider.getJdbi().onDemand(CommentDao.class);
    }


}
