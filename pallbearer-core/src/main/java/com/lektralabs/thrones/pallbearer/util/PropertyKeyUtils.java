package com.lektralabs.thrones.pallbearer.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting property keys from dot notation to camelCase
 */
public class PropertyKeyUtils {

    /**
     * Converts a dot-separated key to camelCase
     * Examples:
     * - "user.drill.group" -> "userDrillGroup"
     * - "user.registration.payment.state" -> "userRegistrationPaymentState"
     * - "user.metric.drill.group.completion.percent" -> "userMetricDrillGroupCompletionPercent"
     */
    public static String toCamelCase(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        
        String[] parts = key.split("\\.");
        if (parts.length == 1) {
            return parts[0];
        }
        
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)))
                      .append(parts[i].substring(1));
            }
        }
        
        return result.toString();
    }

    /**
     * Converts all keys in a Map from dot notation to camelCase
     */
    public static Map<String, String> convertKeysToCamelCase(Map<String, String> originalMap) {
        if (originalMap == null) {
            return new HashMap<>();
        }
        
        Map<String, String> convertedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : originalMap.entrySet()) {
            String camelCaseKey = toCamelCase(entry.getKey());
            convertedMap.put(camelCaseKey, entry.getValue());
        }
        
        return convertedMap;
    }

    /**
     * Converts all keys in nested Maps from dot notation to camelCase
     */
    public static Map<String, Map<String, String>> convertNestedKeysToCamelCase(Map<String, Map<String, String>> originalMap) {
        if (originalMap == null) {
            return new HashMap<>();
        }
        
        Map<String, Map<String, String>> convertedMap = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : originalMap.entrySet()) {
            String groupName = entry.getKey();
            Map<String, String> groupProperties = convertKeysToCamelCase(entry.getValue());
            convertedMap.put(groupName, groupProperties);
        }
        
        return convertedMap;
    }
}
