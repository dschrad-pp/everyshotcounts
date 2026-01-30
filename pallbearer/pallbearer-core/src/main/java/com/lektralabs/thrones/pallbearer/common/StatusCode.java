package com.lektralabs.thrones.pallbearer.common;

import java.util.Optional;

/**
 * ACTIVE  - typical active record; default
 * ARCHIVE - keep the record, but archive it; do not show in regular queries
 * PENDING - record will become active at some point
 * DELETED - record is now deleted; keep in database but will not surface again in UI
 *
 * INACTIVE - like PENDING; record was once ACTIVE but is not, now inactive; needs to be brought into ACTIVE by some action
 * DISABLED - like DELETED; do not surface in UI; record may become ACTIVE after some action
 */
public enum StatusCode {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    ARCHIVE("ARCHIVE"),
    PENDING("PENDING"),
    DELETED("DELETED"),
    DISABLED("DISABLED");

    private final String value;

    StatusCode(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StatusCode fromString(String text) {
        if (Optional.ofNullable(text).isPresent()) {
            for (StatusCode current: StatusCode.values()) {
                if (text.equalsIgnoreCase(current.value)) {
                    return current;
                }
            }
        }
        throw new IllegalArgumentException(String.format("Could not convert [%s] to StatusCode.", text));
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static String[] getDefault() {
        String[] defaultStatusCodes = { StatusCode.ACTIVE.getValue() };
        return  defaultStatusCodes;
    }

    @Override
    public String toString(){
        return value;
    }

}
