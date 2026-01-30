package com.lektralabs.thrones.pallbearer.util;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PropertyKeyUtils
 */
public class PropertyKeyUtilsTest {

    @Test
    public void testToCamelCase() {
        // Test basic dot-separated keys
        assertEquals("userDrillGroup", PropertyKeyUtils.toCamelCase("user.drill.group"));
        assertEquals("userRegistrationPaymentState", PropertyKeyUtils.toCamelCase("user.registration.payment.state"));
        assertEquals("userMetricDrillGroupCompletionPercent", PropertyKeyUtils.toCamelCase("user.metric.drill.group.completion.percent"));
        
        // Test single word
        assertEquals("username", PropertyKeyUtils.toCamelCase("username"));
        
        // Test empty and null
        assertEquals("", PropertyKeyUtils.toCamelCase(""));
        assertNull(PropertyKeyUtils.toCamelCase(null));
    }

    @Test
    public void testConvertKeysToCamelCase() {
        Map<String, String> originalMap = new HashMap<>();
        originalMap.put("user.drill.group", "aac04c9c-b71a-47af-8cc2-861ce4cd5acd");
        originalMap.put("user.registration.payment.state", "PAID");
        originalMap.put("user.drill.group.name", "Beginner");
        originalMap.put("user.registration.sixdigit.code", "743973");
        originalMap.put("user.registration.state", "REGISTERED");
        originalMap.put("user.registration.date", "1754024394334");
        originalMap.put("user.registration.step", "step_1_6");

        Map<String, String> convertedMap = PropertyKeyUtils.convertKeysToCamelCase(originalMap);

        assertEquals("aac04c9c-b71a-47af-8cc2-861ce4cd5acd", convertedMap.get("userDrillGroup"));
        assertEquals("PAID", convertedMap.get("userRegistrationPaymentState"));
        assertEquals("Beginner", convertedMap.get("userDrillGroupName"));
        assertEquals("743973", convertedMap.get("userRegistrationSixdigitCode"));
        assertEquals("REGISTERED", convertedMap.get("userRegistrationState"));
        assertEquals("1754024394334", convertedMap.get("userRegistrationDate"));
        assertEquals("step_1_6", convertedMap.get("userRegistrationStep"));
    }

    @Test
    public void testConvertNestedKeysToCamelCase() {
        Map<String, Map<String, String>> originalMap = new HashMap<>();
        
        Map<String, String> beginnerGroup = new HashMap<>();
        beginnerGroup.put("user.drill.group.level", "7.0");
        beginnerGroup.put("user.drill.group.order.index", "10.0");
        beginnerGroup.put("user.metric.drill.group.completion.percent", "18");
        beginnerGroup.put("user.metric.drill.level.completion.percent", "70");
        
        Map<String, String> advanceGroup = new HashMap<>();
        advanceGroup.put("user.drill.group.level", "1.0");
        advanceGroup.put("user.drill.group.order.index", "1.0");
        advanceGroup.put("user.metric.drill.group.completion.percent", "0");
        advanceGroup.put("user.metric.drill.level.completion.percent", "0");
        
        originalMap.put("Beginner", beginnerGroup);
        originalMap.put("Advance", advanceGroup);

        Map<String, Map<String, String>> convertedMap = PropertyKeyUtils.convertNestedKeysToCamelCase(originalMap);

        // Test Beginner group
        Map<String, String> convertedBeginner = convertedMap.get("Beginner");
        assertEquals("7.0", convertedBeginner.get("userDrillGroupLevel"));
        assertEquals("10.0", convertedBeginner.get("userDrillGroupOrderIndex"));
        assertEquals("18", convertedBeginner.get("userMetricDrillGroupCompletionPercent"));
        assertEquals("70", convertedBeginner.get("userMetricDrillLevelCompletionPercent"));

        // Test Advance group
        Map<String, String> convertedAdvance = convertedMap.get("Advance");
        assertEquals("1.0", convertedAdvance.get("userDrillGroupLevel"));
        assertEquals("1.0", convertedAdvance.get("userDrillGroupOrderIndex"));
        assertEquals("0", convertedAdvance.get("userMetricDrillGroupCompletionPercent"));
        assertEquals("0", convertedAdvance.get("userMetricDrillLevelCompletionPercent"));
    }
}
