package com.lektralabs.thrones.pallbearer.entity;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public interface EntityMethods {

    default <T> T optionalFactory(Optional<T> input){
        if (input == null || input.isEmpty()) {
            return null;
        } else {
            return (T) input.get();
        }
    }

    default <T> T optionalFactory(Optional<T> input, T defaultValue){
        if (input == null || input.isEmpty()) {
            return defaultValue;
        } else {
            return input.get();
        }
    }

    default <T> T optionalFactoryString(Optional<T> input, T defaultValue) {
        if (input != null && !input.isEmpty()) {
            if (input.isPresent() && input.get().getClass() == String.class) {
                String stringValue = (String)input.get();
                return StringUtils.isNotEmpty(stringValue) ? (T) stringValue : defaultValue;
            } else {
                return input.get();
            }
        } else {
            return defaultValue;
        }
    }

}
