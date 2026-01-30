package com.lektralabs.thrones.leagueapps.handlers;

import com.lektralabs.leagueapps.ApiHandler;
import com.lektralabs.leagueapps.LeagueAppsConstants;
import com.lektralabs.thrones.leagueapps.model.json.Registration;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsRegistrationPartial;
import com.lektralabs.thrones.pallbearer.datetime.NumberUtils;
import com.lektralabs.thrones.pallbearer.jdbi.service.LeagueAppsRegistrationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.json.JSONArray;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class RegistrationHandler extends ApiHandler implements LeagueAppsConstants {
    private static final Logger logger = Logger.getLogger(RegistrationHandler.class);

    public RegistrationHandler() {
        super();
    }

    @Inject
    LeagueAppsRegistrationService leagueAppsRegistrationService;

    @Override
    public String getEndpoint() {
        return REGISTRATIONS2_RECORD_TYPE;
    }

    @Override
    public void handlePayload(int batchCount, String responseBody) throws IOException {
        handlePayloadDatabase(batchCount, responseBody);
        // handlePayloadJson(batchCount, responseBody);
    }

    private void handlePayloadDatabase(int batchCount, String responseBody) throws IOException {
        List<Registration> registrations = objectMapper.readValue(responseBody,
                typeFactory.constructCollectionType(List.class, Registration.class));

        registrations.forEach(registration -> {
            LeagueAppsRegistrationPartial partial = registration.toPartial();
            createOrUpdate(partial);
        });
    }

    public void handlePayloadJson(int batchCount, String responseBody) throws IOException {
        JSONArray records = new JSONArray(responseBody);
        logger.info("Processing batch [%d] out of [%d] records".formatted(batchCount, records.length()));
        try (FileWriter file = new FileWriter("registration-records.json")) {
            file.write(records.toString(4));
        }
    }

    public Long createOrUpdate(LeagueAppsRegistrationPartial partial) {
        if (partial.getRegistrationId().isPresent() && NumberUtils.isValidId(partial.getRegistrationId().get())) {
            long registrationId = partial.getRegistrationId().get();
            return leagueAppsRegistrationService.findByRegistrationId(registrationId).map(
                    row -> (long) leagueAppsRegistrationService.update(partial)
            ).orElseGet(() -> leagueAppsRegistrationService.create(partial));
        } else {
            logger.error("No registration found for record [%s]. This is invalid and will be skipped.".formatted(partial));
        }
        return -1L;

    }

}
