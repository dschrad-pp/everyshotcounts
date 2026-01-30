package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.dao.EscLeagueAppsMemberActionDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.EscActionItem;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.EscLeagueAppsMemberActionBaseService;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class EscLeagueAppsMemberActionService extends EscLeagueAppsMemberActionBaseService {

    private static final Logger logger = Logger.getLogger(EscLeagueAppsMemberActionService.class);

    private EscLeagueAppsMemberActionDao escLeagueAppsMemberActionDao;

    @PostConstruct
    public void init() {
        super.init();
        this.escLeagueAppsMemberActionDao = jdbiProvider.getJdbi().onDemand(EscLeagueAppsMemberActionDao.class);
    }

    public int updateActions(long lastUpdated) {
        int rv = jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                return escLeagueAppsMemberActionDao.updateActions(lastUpdated);
            } catch (Exception e) {
                logger.warn("Error updating a [updateActions]", e);
                throw new TransactionException(e);
            }
        });
        return rv;
    }

    public List<EscActionItem> getActionItems() {
        try {
            List<EscActionItem> actionItems = escLeagueAppsMemberActionDao.getActionItems();
            if (actionItems != null) {
                logger.info("Retrieved members count : " + actionItems.size());
                return actionItems;
            } else {
                logger.info("No action items found");
                return java.util.Collections.emptyList();
            }
        } catch (Exception e) {
            logger.warn("Error retrieving action items: " + e.getMessage(), e);
            // Return empty list instead of throwing to allow integration to continue
            return java.util.Collections.emptyList();
        }
    }

    public List<EscActionItem> getAllActionItems() {
        try {
            List<EscActionItem> actionItems = escLeagueAppsMemberActionDao.getAllActionItems();
            if (actionItems != null) {
                logger.info("Retrieved total action items count : " + actionItems.size());
                return actionItems;
            } else {
                logger.info("No action items found");
                return java.util.Collections.emptyList();
            }
        } catch (Exception e) {
            logger.warn("Error retrieving all action items: " + e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

}
