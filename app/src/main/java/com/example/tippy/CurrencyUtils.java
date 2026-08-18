package com.example.tippy;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CurrencyUtils {

    public static final String NO_SYMBOL = "";

    private static final Pattern SYMBOL_BEFORE_AMOUNT = Pattern.compile(
            "([€£$¥₹]|CHF|kr|R\\$)\\s*\\d"
    );

    private CurrencyUtils() {
    }

    public static String detectFromReceipt(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return NO_SYMBOL;
        }

        Map<String, Integer> counts = new HashMap<>();
        Matcher matcher = SYMBOL_BEFORE_AMOUNT.matcher(rawText);
        while (matcher.find()) {
            String symbol = matcher.group(1);
            counts.put(symbol, counts.getOrDefault(symbol, 0) + 1);
        }

        String best = NO_SYMBOL;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    public static String format(String symbol, double amount) {
        String formattedAmount = String.format(Locale.US, "%.2f", amount);
        if (symbol == null || symbol.isBlank()) {
            return formattedAmount;
        }

        if ("kr".equalsIgnoreCase(symbol) || "CHF".equalsIgnoreCase(symbol)) {
            return formattedAmount + " " + symbol;
        }
        return symbol + formattedAmount;
    }
}
