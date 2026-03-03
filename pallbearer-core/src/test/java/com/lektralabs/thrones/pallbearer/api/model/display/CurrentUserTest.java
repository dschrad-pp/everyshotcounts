package com.lektralabs.thrones.pallbearer.api.model.display;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CurrentUser flattened group properties
 */
public class CurrentUserTest {

    @Test
    public void testFlattenedGroupProperties() {
        // Create test data
        UUID userId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        
        Map<String, String> userProperties = new HashMap<>();
        userProperties.put("user.drill.group", "test-group-id");
        userProperties.put("user.registration.state", "REGISTERED");
        
        Map<String, Map<String, String>> groupProperties = new HashMap<>();
        
        // Beginner group properties
        Map<String, String> beginnerProps = new HashMap<>();
        beginnerProps.put("user.drill.group.level", "7.0");
        beginnerProps.put("user.drill.group.order.index", "10.0");
        beginnerProps.put("user.metric.drill.group.completion.percent", "18");
        beginnerProps.put("user.metric.drill.level.completion.percent", "70");
        groupProperties.put("Beginner", beginnerProps);
        
        // Advance group properties
        Map<String, String> advanceProps = new HashMap<>();
        advanceProps.put("user.drill.group.level", "1.0");
        advanceProps.put("user.drill.group.order.index", "1.0");
        advanceProps.put("user.metric.drill.group.completion.percent", "0");
        advanceProps.put("user.metric.drill.level.completion.percent", "0");
        groupProperties.put("Advance", advanceProps);
        
        // Intermediate group properties
        Map<String, String> intermediateProps = new HashMap<>();
        intermediateProps.put("user.drill.group.level", "1.0");
        intermediateProps.put("user.drill.group.order.index", "1.0");
        intermediateProps.put("user.metric.drill.group.completion.percent", "1");
        intermediateProps.put("user.metric.drill.level.completion.percent", "10");
        groupProperties.put("Intermediate", intermediateProps);
        
        // Elite group properties
        Map<String, String> eliteProps = new HashMap<>();
        eliteProps.put("user.drill.group.level", "1.0");
        eliteProps.put("user.drill.group.order.index", "1.0");
        eliteProps.put("user.metric.drill.group.completion.percent", "0");
        eliteProps.put("user.metric.drill.level.completion.percent", "0");
        groupProperties.put("Elite", eliteProps);
        
        // Create CurrentUser instance
        CurrentUser currentUser = new CurrentUser(
            userId, keycloakId, null, null, "testuser", 
            "test@example.com", "Test", "User", "ATHLETE", "ATHLETE", 
            "step_1_6", new HashMap<>(), userProperties, groupProperties,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        
        // Test that flattened properties are initialized
        assertNotNull(currentUser.getBeginnerGroupProperties());
        assertNotNull(currentUser.getIntermediateGroupProperties());
        assertNotNull(currentUser.getAdvanceGroupProperties());
        assertNotNull(currentUser.getEliteGroupProperties());
        
        // Test that they are empty by default (since we're not setting them in constructor)
        assertTrue(currentUser.getBeginnerGroupProperties().isEmpty());
        assertTrue(currentUser.getIntermediateGroupProperties().isEmpty());
        assertTrue(currentUser.getAdvanceGroupProperties().isEmpty());
        assertTrue(currentUser.getEliteGroupProperties().isEmpty());
        
        // Test setting flattened properties
        Map<String, String> testBeginnerProps = new HashMap<>();
        testBeginnerProps.put("userDrillGroupLevel", "7.0");
        testBeginnerProps.put("userDrillGroupOrderIndex", "10.0");
        testBeginnerProps.put("userMetricDrillGroupCompletionPercent", "18");
        testBeginnerProps.put("userMetricDrillLevelCompletionPercent", "70");
        
        currentUser.setBeginnerGroupProperties(testBeginnerProps);
        
        assertEquals("7.0", currentUser.getBeginnerGroupProperties().get("userDrillGroupLevel"));
        assertEquals("10.0", currentUser.getBeginnerGroupProperties().get("userDrillGroupOrderIndex"));
        assertEquals("18", currentUser.getBeginnerGroupProperties().get("userMetricDrillGroupCompletionPercent"));
        assertEquals("70", currentUser.getBeginnerGroupProperties().get("userMetricDrillLevelCompletionPercent"));
    }
}
