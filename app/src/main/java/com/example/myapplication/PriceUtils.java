package com.example.myapplication;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/** Formats product amounts with Colombian thousands and decimal separators. */
public final class PriceUtils {

    private PriceUtils() {
    }

    public static String format(Object amount) {
        if (amount == null) return "0";

        try {
            BigDecimal value = new BigDecimal(amount.toString()).stripTrailingZeros();
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            symbols.setDecimalSeparator(',');
            DecimalFormat formatter = new DecimalFormat("#,##0.##", symbols);
            formatter.setGroupingUsed(true);
            return formatter.format(value);
        } catch (NumberFormatException ignored) {
            return amount.toString();
        }
    }
}
