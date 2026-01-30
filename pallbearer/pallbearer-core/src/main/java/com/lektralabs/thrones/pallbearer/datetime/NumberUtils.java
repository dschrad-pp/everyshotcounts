package com.lektralabs.thrones.pallbearer.datetime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NumberUtils {

    public static Optional<Long> toLong(String str) {
        Optional<Long> rv = Optional.empty();
        try {
            rv = Optional.ofNullable(Long.parseLong(str));
        } catch (NumberFormatException nfe) {
            // do nothing
        }
        return rv;
    }

    public static long toLong(String str, long defaultValue) {
        long rv = defaultValue;
        try {
            rv = Long.parseLong(str);
        } catch (NumberFormatException nfe) {
            // do nothing
        }
        return rv;
    }

    public static int toInt(String str, int defaultValue) {
        int rv = defaultValue;
        try {
            rv = Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            // do nothing
        }
        return rv;
    }


    public static long getLong(Map<String, List<String>> params, String key, long defaultValue) {
        long rv = defaultValue;
        if (params.containsKey(key)) {
            if (Optional.ofNullable(params.get(key)).isPresent()){
                List<String> value = params.get(key);
                if (value.size() == 1) {
                    rv = toLong(value.get(0), defaultValue);
                }
            }
        }
        return rv;
    }

    public static boolean isValidId(long input) {
        return (input != 0L && input != -1L);
    }

    public static boolean isValidId(String input) {
        return isValidId(toLong(input, 0L));
    }


}
