package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.FinancialAccountDao;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.FinancialAccountBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// This is generated code. You are free to modify - it will not be overwritten

@ApplicationScoped
public class FinancialAccountService extends FinancialAccountBaseService {
    private static final Logger logger = Logger.getLogger(FinancialAccountService.class);

    private FinancialAccountDao financialAccountDao;

    @PostConstruct
    public void init() {
        super.init();
        // Uncomment this when you have added methods to the new DAO
        // this.financialAccountDao = jdbiProvider.getJdbi().onDemand(FinancialAccountDao.class);
    }


}
