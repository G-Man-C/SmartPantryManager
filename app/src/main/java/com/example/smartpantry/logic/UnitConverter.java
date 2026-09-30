package com.example.smartpantry.logic;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Converts between cooking units so that "1 kg" in the pantry can satisfy "500 g" in a recipe.
 *
 * Every unit belongs to a category (mass, volume or count) and has a factor that converts it
 * to that category's base unit (grams, millilitres or pieces). Quantities are only ever
 * compared inside the same category: grams can never satisfy a requirement in millilitres,
 * because that would need the density of each food and could produce false matches.
 */
public final class UnitConverter {

    public enum Category { MASS, VOLUME, COUNT }

    public enum Unit {
        G("g", Category.MASS, 1),
        KG("kg", Category.MASS, 1000),
        ML("ml", Category.VOLUME, 1),
        L("l", Category.VOLUME, 1000),
        TSP("tsp", Category.VOLUME, 5),
        TBSP("tbsp", Category.VOLUME, 15),
        CUP("cup", Category.VOLUME, 250),
        PCS("pcs", Category.COUNT, 1);

        public final String symbol;
        public final Category category;
        public final double factorToBase;

        Unit(String symbol, Category category, double factorToBase) {
            this.symbol = symbol;
            this.category = category;
            this.factorToBase = factorToBase;
        }
    }

    /** Every spelling we accept for each unit, so typed or legacy values still parse. */
    private static final Map<String, Unit> ALIASES = new HashMap<>();

    static {
        register(Unit.G, "g", "gram", "grams", "gr", "gm", "gms");
        register(Unit.KG, "kg", "kgs", "kilogram", "kilograms", "kilo", "kilos");
        register(Unit.ML, "ml", "mls", "millilitre", "millilitres", "milliliter", "milliliters");
        register(Unit.L, "l", "lt", "ltr", "litre", "litres", "liter", "liters");
        register(Unit.TSP, "tsp", "tsps", "teaspoon", "teaspoons");
        register(Unit.TBSP, "tbsp", "tbsps", "tbs", "tablespoon", "tablespoons");
        register(Unit.CUP, "cup", "cups", "c");
        register(Unit.PCS, "pcs", "pc", "piece", "pieces", "unit", "units", "item", "items",
                "x", "each", "whole", "clove", "cloves", "slice", "slices");
    }

    private UnitConverter() {
    }

    private static void register(Unit unit, String... names) {
        for (String n : names) {
            ALIASES.put(n, unit);
        }
    }

    /** Parses a unit string leniently ("Grams", "kg.", " ML "). Returns null if unknown. */
    public static Unit parse(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace(".", "");
        if (key.isEmpty()) {
            return null;
        }
        return ALIASES.get(key);
    }

    /** Converts a quantity to its category's base unit (g, ml or pcs). */
    public static double toBase(double quantity, Unit unit) {
        return quantity * unit.factorToBase;
    }

    public static String baseSymbol(Category category) {
        switch (category) {
            case MASS:
                return "g";
            case VOLUME:
                return "ml";
            default:
                return "pcs";
        }
    }

    /** The unit symbols in display order, used to fill the unit dropdowns. */
    public static String[] symbols() {
        Unit[] units = Unit.values();
        String[] result = new String[units.length];
        for (int i = 0; i < units.length; i++) {
            result[i] = units[i].symbol;
        }
        return result;
    }

    /** Formats 2.0 as "2" and 0.5 as "0.5", with at most two decimal places. */
    public static String format(double quantity) {
        if (quantity == Math.rint(quantity)) {
            return String.valueOf((long) quantity);
        }
        String s = String.format(Locale.ROOT, "%.2f", quantity);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
}
