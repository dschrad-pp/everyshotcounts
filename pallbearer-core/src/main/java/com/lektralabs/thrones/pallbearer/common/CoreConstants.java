package com.lektralabs.thrones.pallbearer.common;

import java.util.Map;
import java.util.UUID;

public interface CoreConstants {

    UUID SYSTEM_USER_ID = UUID.fromString("d79ab826-65de-4fda-8b5f-779dacfe00fe");
    UUID THRONES_ORG_ID = UUID.fromString("55b53d7c-3137-4e00-ab1b-179fd7c8d41a");

    UUID INVALID_PLACEHOLDER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

//     UUID ADMIN_ROLE_ID = UUID.fromString("cfd5afed-e04d-41d5-bc31-41d94f23ad7c");
    UUID ADMIN_ROLE_ID = UUID.fromString("559e567f-4c88-4b00-a593-a3e64086f603");
//     UUID USER_ROLE_ID = UUID.fromString("411ce934-0b3f-4590-a38d-3b77581cb2ad");
    UUID USER_ROLE_ID = UUID.fromString("6bf20ac0-1cf3-4e84-83b8-de15860b16f8");
//     UUID ATHLETE_ROLE_ID = UUID.fromString("b2f70816-f2fe-45d4-87c0-8d9c6ab33d7a");
    UUID ATHLETE_ROLE_ID = UUID.fromString("67452c9c-0454-44d5-b844-8d235005e99a");
//     UUID FAN_ROLE_ID = UUID.fromString("f05b85ea-303e-407b-b342-c2478a91a52d");
    UUID FAN_ROLE_ID = UUID.fromString("8721d44c-bdf9-404b-89d3-889c25b49316");
//     UUID COACH_ROLE_ID = UUID.fromString("840d3e48-340d-45a0-85ca-4ddf01dc0877");
    UUID COACH_ROLE_ID = UUID.fromString("802a6427-0b26-4267-a187-bef31a36f2ab");

    UUID EVERY_SHOT_COUNTS_TEAM_ID = UUID.fromString("fc793899-489c-4bd9-bf80-616e1318e714");

    Map<String, UUID> ROLE_TO_ID = Map.of(
            "ADMIN", ADMIN_ROLE_ID,
            "USER", USER_ROLE_ID,
            "ATHLETE", ATHLETE_ROLE_ID,
            "FAN", FAN_ROLE_ID,
            "COACH", COACH_ROLE_ID
    );

//     UUID ADMIN_GROUP_ID = UUID.fromString("7f85ddfe-6566-42e0-aa1a-6b3f15f12050");
    UUID ADMIN_GROUP_ID = UUID.fromString("40e31ac9-90b0-4681-a8fc-c5f3b7163ee7");
//     UUID USER_GROUP_ID = UUID.fromString("64cb29bd-44a4-4b2a-9b92-d2c63e446be2");
    UUID USER_GROUP_ID = UUID.fromString("13d5ed42-de73-485d-b175-3de63de6e33f");
//     UUID ATHLETE_GROUP_ID = UUID.fromString("dc609b09-5ac5-4e6b-b66c-d748890a85cb");
    UUID ATHLETE_GROUP_ID = UUID.fromString("94665bbc-5f81-40eb-8d3b-c4ae4bb37985");
//     UUID FAN_GROUP_ID = UUID.fromString("c5a52846-6408-4396-9f2e-22b14a32cea3");
    UUID FAN_GROUP_ID = UUID.fromString("e3e77f80-9d30-4c18-972b-00e97fbf288a");
//     UUID COACH_GROUP_ID = UUID.fromString("e1d9f28b-37e8-4476-82e6-c88556968a11");
    UUID COACH_GROUP_ID = UUID.fromString("db0e94d3-36ec-4f1c-b727-c1eae427e823");

    Map<String, UUID> GROUP_TO_ID = Map.of(
            "ADMIN", ADMIN_GROUP_ID,
            "USER", USER_GROUP_ID,
            "ATHLETE", ATHLETE_GROUP_ID,
            "FAN", FAN_GROUP_ID,
            "COACH", COACH_GROUP_ID
    );
}
