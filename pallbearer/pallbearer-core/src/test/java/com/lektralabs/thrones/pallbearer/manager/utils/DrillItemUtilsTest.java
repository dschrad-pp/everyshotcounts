package com.lektralabs.thrones.pallbearer.manager.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DrillItemUtilsTest {

    @Test
    void testLevelIdentifierToInt() {
        assertEquals(1, DrillItemUtils.levelIdentifierToInt("1"));
        assertEquals(1, DrillItemUtils.levelIdentifierToInt(null));
        assertEquals(1, DrillItemUtils.levelIdentifierToInt("1.0"));
        assertEquals(2, DrillItemUtils.levelIdentifierToInt("2.5"));
        assertEquals(3, DrillItemUtils.levelIdentifierToInt("3.1415926535"));
    }
}
