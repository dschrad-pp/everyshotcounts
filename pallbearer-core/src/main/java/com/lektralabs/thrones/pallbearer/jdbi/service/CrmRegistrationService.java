package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.CrmRegistrationDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.CrmRegistrationRow;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CrmRegistrationService {
    private static final Logger logger = Logger.getLogger(CrmRegistrationService.class);

    @Inject
    JdbiProvider jdbiProvider;

    private CrmRegistrationDao crmRegistrationDao;

    @PostConstruct
    public void init() {
        this.crmRegistrationDao = jdbiProvider.getJdbi().onDemand(CrmRegistrationDao.class);
    }

    public Optional<CrmRegistrationRow> findByRegistrationId(long registrationId) {
        return crmRegistrationDao.findByRegistrationId(registrationId);
    }

    public List<CrmRegistrationRow> findAll() {
        return crmRegistrationDao.findAll();
    }

    public CrmRegistrationRow createOrUpdate(CrmRegistrationRow row) {
        Optional<CrmRegistrationRow> existing = crmRegistrationDao.findByRegistrationId(row.getRegistrationId());
        if (existing.isPresent()) {
            crmRegistrationDao.update(row);
            return row;
        } else {
            crmRegistrationDao.insert(row);
            return row;
        }
    }
}
