package com.lektralabs.thrones.pallbearer.common;

import java.util.Map;
import java.util.UUID;

public interface DrillGroupConstants {

    UUID BEGINNER_GROUP_ID = UUID.fromString("aac04c9c-b71a-47af-8cc2-861ce4cd5acd");
    UUID INTERMEDIATE_GROUP_ID = UUID.fromString("6ccc50a3-f356-416e-98db-ed50e58e976b");
    UUID ADVANCE_GROUP_ID = UUID.fromString("ad8903ec-e765-4f0e-a6a2-359337f37b82");
    UUID ELITE_GROUP_ID = UUID.fromString("4f60b437-18d4-44b4-a975-574eea5acc88");

    Map<UUID, String> drillGroupIdNameMap = Map.of(
            BEGINNER_GROUP_ID, "Beginner",
            INTERMEDIATE_GROUP_ID, "Intermediate",
            ADVANCE_GROUP_ID, "Advanced",
            ELITE_GROUP_ID, "Elite");
}
