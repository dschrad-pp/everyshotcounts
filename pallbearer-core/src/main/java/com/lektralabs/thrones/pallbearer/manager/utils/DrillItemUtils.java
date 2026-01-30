package com.lektralabs.thrones.pallbearer.manager.utils;

import com.lektralabs.thrones.pallbearer.datetime.NumberUtils;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;

/**
 * Provides drill item helpers
 */
public class DrillItemUtils {

    public static String levelIdentifier(Integer levelIndex, Boolean levelTest) {
        if (levelIndex != null) {
            String indexStr = String.valueOf(levelIndex);
            if (levelTest != null && levelTest) {
                return indexStr + ".0";
            } else {
                return indexStr + ".0";
            }
        } else {
            // should not happen, but just in case
            return "1.0";
        }
    }

    public static String orderIndexIdentifier(Integer orderIndex, Boolean levelTest) {
        if (orderIndex != null) {
            String indexStr = String.valueOf(orderIndex);
            if (levelTest != null && levelTest) {
                return indexStr + ".0";
            } else {
                return indexStr + ".0";
            }
        } else {
            // should not happen, but just in case
            return "1.0";
        }
    }

    public static String levelIdentifier(DrillItemRow drillItemRow) {
        return levelIdentifier(drillItemRow.getLevelIndex(),
                drillItemRow.getLevelTest());
    }

    public static String levelIdentifier(DrillItemDetail drillItemDetail) {
        return levelIdentifier(drillItemDetail.getLevelIndex(),
                drillItemDetail.getLevelTest());
    }

    public static String levelIdentifier(AthleteDrillDetail athleteDrillDetail) {
        return levelIdentifier(athleteDrillDetail.getLevelIndex(),
                athleteDrillDetail.getLevelTest());
    }

    public static int levelIdentifierToInt(String levelIdentifier) {
        if (levelIdentifier != null) {
            String intStr = levelIdentifier.replaceAll("\\..*?$", "");
            return NumberUtils.toInt(intStr, 1);
        } else {
            return 1;
        }
    }

    public static int orderIndexIdentifierToInt(String orderIndexIdentifier) {
        if (orderIndexIdentifier != null) {
            String intStr = orderIndexIdentifier.replaceAll("\\..*?$", "");
            return NumberUtils.toInt(intStr, 1);
        } else {
            return 1;
        }
    }
}
