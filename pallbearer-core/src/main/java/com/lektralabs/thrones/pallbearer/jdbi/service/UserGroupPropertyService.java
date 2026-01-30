package com.lektralabs.thrones.pallbearer.jdbi.service;

import java.util.List;

import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.UserGroupPropertyDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserGroupPropertyRow;
import com.lektralabs.thrones.pallbearer.api.model.partial.UserGroupPropertyPartial;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserGroupPropertyService {

    private static final Logger logger = Logger.getLogger(UserGroupPropertyService.class);

    @Inject
    JdbiProvider jdbiProvider;

    private UserGroupPropertyDao userGroupPropertyDao;

    @PostConstruct
    public void init() {
        this.userGroupPropertyDao = jdbiProvider.getJdbi().onDemand(UserGroupPropertyDao.class);
    }

    @Transactional
    public UUID create(UserGroupPropertyPartial partial) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                UserGroupPropertyRow row = partial.toRow();
                userGroupPropertyDao.deleteByKey(row.getDrillGroupId(), row.getPropertyKey());
                return userGroupPropertyDao.insert(row);
            } catch (Exception e) {
                logger.warn("Error creating [userGroupProperty]", e);
                throw new TransactionException(e);
            }
        });
    }

    @Transactional
    public int update(UserGroupPropertyPartial partial) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                UserGroupPropertyRow row = partial.toRow();
                return userGroupPropertyDao.update(row);
            } catch (Exception e) {
                logger.warn("Error updating [userGroupProperty]", e);
                throw new TransactionException(e);
            }
        });
    }

    @Transactional
    public int update(UserGroupPropertyRow row) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                return userGroupPropertyDao.update(row);
            } catch (Exception e) {
                logger.warn("Error updating [userGroupProperty]", e);
                throw new TransactionException(e);
            }
        });
    }

    public List<UserGroupPropertyRow> findByUserAndGroup(UUID userId, UUID groupId) {
        return userGroupPropertyDao.findByUserAndGroup(userId, groupId);
    }

    public Optional<UserGroupPropertyRow> findById(UUID id) {
        return userGroupPropertyDao.findById(id);
    }

    public Optional<UserGroupPropertyRow> findByKey(UUID userId, UUID groupId, String propertyKey) {
        return userGroupPropertyDao.findByKey(userId, groupId, propertyKey);
    }

    public void addProperties(UUID groupId, Map<String, String> groupPropertyMap) {
        groupPropertyMap.forEach((key, value) -> {
            create(UserGroupPropertyPartial.builder()
                    .groupId(groupId)
                    .propertyKey(key)
                    .propertyValue(value)
                    .build());
        });
    }
}
