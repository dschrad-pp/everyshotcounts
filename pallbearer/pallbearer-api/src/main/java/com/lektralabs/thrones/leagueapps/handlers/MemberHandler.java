package com.lektralabs.thrones.leagueapps.handlers;

import com.lektralabs.leagueapps.ApiHandler;
import com.lektralabs.leagueapps.LeagueAppsConstants;
import com.lektralabs.thrones.leagueapps.model.json.Member;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsMemberPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.LeagueAppsMemberService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.json.JSONArray;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;

@ApplicationScoped
public class MemberHandler extends ApiHandler implements LeagueAppsConstants {

    private static final Logger logger = Logger.getLogger(MemberHandler.class);

    @Inject
    LeagueAppsMemberService leagueAppsMemberService;

    public MemberHandler() {
        super();
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }

    @Override
    public String getEndpoint() {
        return MEMBERS2_RECORD_TYPE;
    }

    @Override
    public void handlePayload(int batchCount, String responseBody) throws IOException {
        logger.infof("Starting processing of %d members in batch %d",
                new JSONArray(responseBody).length(), batchCount);
        handlePayloadDatabase(batchCount, responseBody);
        // handlePayloadJson(batchCount, responseBody);
    }

    private void handlePayloadDatabase(int batchCount, String responseBody) throws IOException {
        List<Member> members = objectMapper.readValue(responseBody,
                typeFactory.constructCollectionType(List.class, Member.class));

        members.forEach(member -> {
            LeagueAppsMemberPartial partial = member.toPartial();
            createOrUpdate(partial);
        });
    }

    public void handlePayloadJson(int batchCount, String responseBody) throws IOException {
        JSONArray records = new JSONArray(responseBody);
        logger.info("Processing batch [%d] out of [%d] records".formatted(batchCount, records.length()));
        try (FileWriter file = new FileWriter("user-records.json")) {
            file.write(records.toString(4));
        }
    }

    public Long createOrUpdate(LeagueAppsMemberPartial partial) {
        if (partial.getUsername().isPresent() && StringUtils.isNotBlank(partial.getUsername().get())) {
            String username = partial.getUsername().get();
            return leagueAppsMemberService.findByUsername(username).map(
                    row -> (long) leagueAppsMemberService.update(partial)
            ).orElseGet(() -> leagueAppsMemberService.create(partial));
        } else {
            logger.error("No username found for record [%s]. This is invalid and will be skipped.".formatted(partial));
        }
        return -1L;
    }

}
