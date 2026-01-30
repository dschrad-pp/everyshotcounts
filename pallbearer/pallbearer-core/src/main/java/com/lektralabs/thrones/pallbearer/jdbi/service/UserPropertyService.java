package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.UserPropertyPartial;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.UserPropertyDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.lektralabs.thrones.pallbearer.common.UserPropertyConstants;

@ApplicationScoped
public class UserPropertyService {

    private static final Logger logger = Logger.getLogger(UserPropertyService.class);

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserService userService;

    private UserPropertyDao userPropertyDao;

    @PostConstruct
    public void init() {
        this.userPropertyDao = jdbiProvider.getJdbi().onDemand(UserPropertyDao.class);
    }

    @Transactional
    public UUID create(UserPropertyPartial userPropertyPartial) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                UserPropertyRow userPropertyRow = userPropertyPartial.toRow();
                userPropertyDao.deleteByKey(userPropertyRow.getUserId(), userPropertyRow.getPropertyKey());
                UUID createdId = userPropertyDao.insert(userPropertyRow);

                // If setting user.drill.group, also set user.drill.group.name
                if (UserPropertyConstants.USER_DRILL_GROUP_KEY.equals(userPropertyRow.getPropertyKey())) {
                    UUID groupId = UUID.fromString(userPropertyRow.getPropertyValue());
                    String groupName = DrillGroupConstants.drillGroupIdNameMap.get(groupId);
                    if (groupName != null) {
                        UserPropertyRow nameRow = UserPropertyRow.builder()
                                .id(UUID.randomUUID())
                                .userId(userPropertyRow.getUserId())
                                .propertyKey(UserPropertyConstants.USER_DRILL_GROUP_NAME_KEY)
                                .propertyValue(groupName)
                                .build();
                        userPropertyDao.deleteByKey(nameRow.getUserId(), nameRow.getPropertyKey());
                        userPropertyDao.insert(nameRow);
                    }
                }

                return createdId;
            } catch (Exception e) {
                logger.warn("Error creating a [userProperty]", e);
                throw new TransactionException(e);
            }
        });
    }

    @Transactional
    public int update(UserPropertyPartial userPropertyPartial) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                UserPropertyRow userPropertyRow = userPropertyPartial.toRow();
                int updated = userPropertyDao.update(userPropertyRow);

                // If updating user.drill.group, also update user.drill.group.name
                if (UserPropertyConstants.USER_DRILL_GROUP_KEY.equals(userPropertyRow.getPropertyKey())) {
                    UUID groupId = UUID.fromString(userPropertyRow.getPropertyValue());
                    String groupName = DrillGroupConstants.drillGroupIdNameMap.get(groupId);
                    if (groupName != null) {
                        UserPropertyRow nameRow = UserPropertyRow.builder()
                                .id(UUID.randomUUID())
                                .userId(userPropertyRow.getUserId())
                                .propertyKey(UserPropertyConstants.USER_DRILL_GROUP_NAME_KEY)
                                .propertyValue(groupName)
                                .build();
                        userPropertyDao.deleteByKey(nameRow.getUserId(), nameRow.getPropertyKey());
                        userPropertyDao.insert(nameRow);
                    }
                }

                return updated;
            } catch (Exception e) {
                logger.warn("Error updating a [userProperty]", e);
                throw new TransactionException(e);
            }
        });
    }

    @Transactional
    public int update(UserPropertyRow userPropertyRow) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                return userPropertyDao.update(userPropertyRow);
            } catch (Exception e) {
                logger.warn("Error updating a [userProperty]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public Optional<UserPropertyRow> findById(UUID id) {
        return userPropertyDao.findById(id);
    }

    public Optional<UserPropertyRow> findByKey(UUID userId, String propertyKey) {
        return userPropertyDao.findByKey(userId, propertyKey);
    }

    public void addProperties(UUID userId, Map<String, String> userPropertyMap) {
        userPropertyMap.forEach((key, value) -> {
            create(UserPropertyPartial.builder()
                    .userId(userId)
                    .propertyKey(key)
                    .propertyValue(value)
                    .build());
        });
    }

}
