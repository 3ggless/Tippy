package com.example.tippy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReceiptParser {

    private static final Pattern LINE_WITH_PRICE = Pattern.compile(
            "^(.+?)\\s+((?:€|£|\\$|¥|₹|CHF|kr|R\\$)?)\\s*(\\d{1,4}[.,]\\d{2})$",
            Pattern.CASE_INSENSITIVE
    );

    private static final String[] SKIP_KEYWORDS = {
            "total", "subtotal", "sub total", "tax", "tip", "gratuity",
            "change", "cash", "card", "visa", "mastercard", "balance",
            "amount", "due", "thank", "receipt", "date", "time"
    };

    private ReceiptParser() {
    }

    public static class ParseResult implements Serializable {
        private final ArrayList<ReceiptItem> items;
        private final String currencySymbol;

        public ParseResult(ArrayList<ReceiptItem> items, String currencySymbol) {
            this.items = items;
            this.currencySymbol = currencySymbol;
        }

        public ArrayList<ReceiptItem> getItems() {
            return items;
        }

        public String getCurrencySymbol() {
            return currencySymbol;
        }
    }

    public static ParseResult parse(String rawText) {
        ArrayList<ReceiptItem> items = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return new ParseResult(items, CurrencyUtils.NO_SYMBOL);
        }

        String currencySymbol = CurrencyUtils.NO_SYMBOL;
        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 3 || shouldSkipLine(trimmed)) {
                continue;
            }

            Matcher matcher = LINE_WITH_PRICE.matcher(trimmed);
            if (matcher.matches()) {
                String name = matcher.group(1).trim();
                String lineSymbol = matcher.group(2);
                if (lineSymbol != null && !lineSymbol.isBlank()) {
                    currencySymbol = lineSymbol.trim();
                }
                double price = parseAmount(matcher.group(3));
                if (price > 0 && price < 10000 && !name.isEmpty()) {
                    items.add(new ReceiptItem(name, price));
                }
            }
        }

        if (CurrencyUtils.NO_SYMBOL.equals(currencySymbol)) {
            currencySymbol = CurrencyUtils.detectFromReceipt(rawText);
        }
        return new ParseResult(items, currencySymbol);
    }

    public static ParseResult sampleResult() {
        ArrayList<ReceiptItem> items = new ArrayList<>();
        items.add(new ReceiptItem("Burger", 14.99));
        items.add(new ReceiptItem("Fries", 4.50));
        items.add(new ReceiptItem("Salad", 11.25));
        items.add(new ReceiptItem("Soda", 3.00));
        items.add(new ReceiptItem("Pasta", 16.75));
        items.add(new ReceiptItem("Wine Glass", 9.50));
        return new ParseResult(items, CurrencyUtils.NO_SYMBOL);
    }

    private static double parseAmount(String amountText) {
        String normalized = amountText.replace(',', '.');
        return Double.parseDouble(normalized);
    }

    private static boolean shouldSkipLine(String line) {
        String lower = line.toLowerCase(Locale.US);
        for (String keyword : SKIP_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
