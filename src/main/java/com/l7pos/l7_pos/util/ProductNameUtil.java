package com.l7pos.l7_pos.util;

public class ProductNameUtil {

    public static String toDisplayName(String productCode) {

        if (productCode == null || productCode.length() < 10) {
            return productCode;
        }

        String season = productCode.substring(2, 4);   // CO2502ST08 -> 25
        String type = productCode.substring(6, 8);     // CO2502ST08 -> ST
        String detail = productCode.substring(8, 10);  // CO2502ST08 -> 08

        return season + "시즌 " + type + " " + detail;
    }
}