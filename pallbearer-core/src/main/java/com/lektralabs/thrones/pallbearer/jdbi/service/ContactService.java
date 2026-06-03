package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.service.generated.ContactBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class ContactService extends ContactBaseService {
    private static final Logger logger = Logger.getLogger(ContactService.class);

    @PostConstruct
    public void init() {
        super.init();
    }


}
