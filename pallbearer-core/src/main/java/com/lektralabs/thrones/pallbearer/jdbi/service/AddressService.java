package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.AddressDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.AddressBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class AddressService extends AddressBaseService {
    private static final Logger logger = Logger.getLogger(AddressService.class);

    private AddressDao addressDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.addressDao = jdbiProvider.getJdbi().onDemand(AddressDao.class);
    }


}
