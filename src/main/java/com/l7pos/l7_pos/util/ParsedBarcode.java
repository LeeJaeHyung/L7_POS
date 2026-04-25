package com.l7pos.l7_pos.util;

public record ParsedBarcode(
        String barcode,
        String productCode,
        String color,
        String size
) {
}