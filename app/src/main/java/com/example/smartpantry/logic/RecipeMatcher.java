package com.example.smartpantry.logic;

import com.example.smartpantry.model.PantryItem;
import com.example.smartpantry.model.Recipe;
import com.example.smartpantry.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strict matching: a recipe is "suggested" only if every ingredient it needs is in the pantry
 * in at least the required quantity. A recipe that is short of exactly one ingredient (missing entirely, or not
 * enough of it) goes into the separate "Almost There" list. Anything short of two or more
 * ingredients appears in neither list.
 *
 * How it works:
 *   1. The pantry is summarised into an index: normalised name -> unit category -> total amount
 *      in base units. Two pantry rows "Eggs 6 pcs" and "egg 2 pcs" add up to 8 pcs of "egg".
 *   2. Each recipe's ingredients are turned into requirements the same way (duplicate lines in
 *      one recipe are added together).
 *   3. Each requirement is compared with the index. It is satisfied only when the available
 *      amount in the same category is at least the required amount.
 */
public final class RecipeMatcher {

    /** Tolerance for floating-point rounding, e.g. 0.1 + 0.2 kg. */
    private static final double EPSILON = 1e-6;

    private RecipeMatcher() {
    }

    /** Totals of what the user has: normalised name -> category -> amount in base units. */
    public static final class PantryIndex {
        private final Map<String, EnumMap<UnitConverter.Category, Double>> totals = new HashMap<>();

        void add(String key, UnitConverter.Category category, double baseAmount) {
            totals.computeIfAbsent(key, k -> new EnumMap<>(UnitConverter.Category.class))
                    .merge(category, baseAmount, Double::sum);
        }

        public double available(String key, UnitConverter.Category category) {
            EnumMap<UnitConverter.Category, Double> byCategory = totals.get(key);
            if (byCategory == null || category == null) {
                return 0;
            }
            Double amount = byCategory.get(category);
            return amount == null ? 0 : amount;
        }
    }

    public static PantryIndex buildIndex(List<PantryItem> pantry) {
        PantryIndex index = new PantryIndex();
        if (pantry == null) {
            return index;
        }
        for (PantryItem item : pantry) {
            if (item == null || item.getQuantity() <= 0) {
                continue;
            }
            UnitConverter.Unit unit = UnitConverter.parse(item.getUnit());
            String key = IngredientNormalizer.normalize(item.getName());
            if (unit == null || key.isEmpty()) {
                continue; // cannot be compared safely, so it cannot satisfy anything
            }
            index.add(key, unit.category, UnitConverter.toBase(item.getQuantity(), unit));
        }
        return index;
    }

    /** One thing a recipe needs, with how much the pantry has of it. */
    public static final class Requirement {
        private final String displayName;
        private final String key;
        /** Null when the recipe uses a unit we do not recognise; such a requirement never passes. */
        private final UnitConverter.Category category;
        private double requiredBase;
        private double availableBase;
        private final List<RecipeIngredient> sources = new ArrayList<>();

        Requirement(String displayName, String key, UnitConverter.Category category) {
            this.displayName = displayName;
            this.key = key;
            this.category = category;
        }

        public boolean isSatisfied() {
            return category != null && availableBase + EPSILON >= requiredBase;
        }

        public double getShortfallBase() {
            return Math.max(0, requiredBase - availableBase);
        }

        public String getBaseUnit() {
            return category == null ? "" : UnitConverter.baseSymbol(category);
        }

        public String getDisplayName() { return displayName; }
        public double getRequiredBase() { return requiredBase; }
        public double getAvailableBase() { return availableBase; }
        public List<RecipeIngredient> getSources() { return sources; }

        /** Human-readable explanation of what is missing, e.g. "Need 20 g more Butter". */
        public String describeShortfall() {
            if (category == null) {
                return "Missing: " + displayName;
            }
            if (availableBase > EPSILON) {
                return "Need " + UnitConverter.format(getShortfallBase()) + " " + getBaseUnit()
                        + " more " + displayName;
            }
            return "Missing: " + displayName + " ("
                    + UnitConverter.format(requiredBase) + " " + getBaseUnit() + ")";
        }
    }

    /** Works out every requirement of a recipe and how much of each the pantry holds. */
    public static List<Requirement> evaluate(Recipe recipe, PantryIndex index) {
        Map<String, Requirement> byKey = new LinkedHashMap<>();
        int unknownCounter = 0;
        for (RecipeIngredient ing : recipe.getIngredients()) {
            String key = IngredientNormalizer.normalize(ing.getName());
            UnitConverter.Unit unit = UnitConverter.parse(ing.getUnit());
            String mapKey = unit == null
                    ? key + "|unknown" + (unknownCounter++)
                    : key + "|" + unit.category;
            Requirement req = byKey.get(mapKey);
            if (req == null) {
                req = new Requirement(ing.getName(), key, unit == null ? null : unit.category);
                byKey.put(mapKey, req);
            }
            if (unit != null) {
                req.requiredBase += UnitConverter.toBase(ing.getQuantity(), unit);
            }
            req.sources.add(ing);
        }
        for (Requirement req : byKey.values()) {
            req.availableBase = index.available(req.key, req.category);
        }
        return new ArrayList<>(byKey.values());
    }

    /** A recipe that is short of exactly one ingredient. */
    public static final class AlmostThere {
        public final Recipe recipe;
        public final Requirement missing;

        AlmostThere(Recipe recipe, Requirement missing) {
            this.recipe = recipe;
            this.missing = missing;
        }
    }

    public static final class Result {
        /** Recipes the user can cook right now (strict matches only). */
        public final List<Recipe> suggested;
        /** Recipes short of exactly one ingredient. Kept completely separate. */
        public final List<AlmostThere> almostThere;

        Result(List<Recipe> suggested, List<AlmostThere> almostThere) {
            this.suggested = suggested;
            this.almostThere = almostThere;
        }
    }

    public static Result match(List<Recipe> recipes, List<PantryItem> pantry) {
        PantryIndex index = buildIndex(pantry);
        List<Recipe> suggested = new ArrayList<>();
        List<AlmostThere> almost = new ArrayList<>();

        for (Recipe recipe : recipes) {
            if (recipe.getIngredients().isEmpty()) {
                continue; // a recipe with no ingredient list can't be verified, so never suggest it
            }
            Requirement firstMissing = null;
            int missingCount = 0;
            for (Requirement req : evaluate(recipe, index)) {
                if (!req.isSatisfied()) {
                    missingCount++;
                    if (firstMissing == null) {
                        firstMissing = req;
                    }
                }
            }
            if (missingCount == 0) {
                suggested.add(recipe);
            } else if (missingCount == 1) {
                almost.add(new AlmostThere(recipe, firstMissing));
            }
        }

        suggested.sort(Comparator.comparing(Recipe::getName, String.CASE_INSENSITIVE_ORDER));
        almost.sort(Comparator.comparing((AlmostThere a) -> a.recipe.getName(), String.CASE_INSENSITIVE_ORDER));
        return new Result(suggested, almost);
    }
}
