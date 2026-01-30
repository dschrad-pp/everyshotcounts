package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.ContactDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.ContactBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class ContactService extends ContactBaseService {
    private static final Logger logger = Logger.getLogger(ContactService.class);

    private ContactDao contactDao;

    @PostConstruct
    public void init() {
        super.init();
        this.contactDao = jdbiProvider.getJdbi().onDemand(ContactDao.class);
    }


}
