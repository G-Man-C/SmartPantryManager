package com.example.smartpantry.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns an ingredient name into a canonical key so that trivially different names still match.
 *
 * Both pantry names and recipe names go through the same steps, so as long as two names
 * normalise to the same key they are treated as the same ingredient:
 *   "Tomatoes"            -> "tomato"
 *   "  Fresh Large EGGS " -> "egg"
 *   "Scallions"           -> "spring onion"
 *   "2 cloves of garlic"  -> handled as "garlic"
 *   "Cake flour"          -> "flour"
 */
public final class IngredientNormalizer {

    /** Describing words that do not change which ingredient it is. */
    private static final Set<String> DESCRIPTORS = new HashSet<>(Arrays.asList(
            "fresh", "large", "small", "medium", "big", "chopped", "diced", "sliced", "grated",
            "crushed", "peeled", "raw", "ripe", "organic", "frozen", "boneless", "skinless",
            "extra", "virgin"));

    /** Leading measure words in phrases like "cloves of garlic" or "slice of bread". */
    private static final Set<String> MEASURE_WORDS = new HashSet<>(Arrays.asList(
            "slice", "slices", "clove", "cloves", "piece", "pieces", "can", "cans", "tin", "tins",
            "bunch", "bunches", "head", "heads", "pinch", "cup", "cups", "packet", "packets"));

    /** Plurals that the simple suffix rules would get wrong. */
    private static final Map<String, String> IRREGULAR = new HashMap<>();

    /** Words ending in "s" that are already singular. */
    private static final Set<String> INVARIANT = new HashSet<>(Arrays.asList(
            "hummus", "couscous", "asparagus", "molasses", "swiss", "oats"));

    /** Different names for the same ingredient, keyed by their normalised form. */
    private static final Map<String, String> SYNONYMS = new HashMap<>();

    static {
        IRREGULAR.put("leaves", "leaf");
        IRREGULAR.put("loaves", "loaf");
        IRREGULAR.put("halves", "half");
        IRREGULAR.put("knives", "knife");

        synonym("spring onion", "scallion", "green onion");
        synonym("coriander", "cilantro");
        synonym("bell pepper", "capsicum", "green pepper", "red pepper", "yellow pepper", "sweet pepper");
        synonym("beef mince", "ground beef", "minced beef", "mince");
        synonym("chicken breast", "chicken fillet");
        synonym("maize meal", "mielie meal", "mealie meal", "mielie pap");
        synonym("zucchini", "courgette", "baby marrow");
        synonym("eggplant", "aubergine", "brinjal");
        synonym("flour", "plain flour", "all purpose flour", "cake flour", "white flour");
        synonym("cheese", "cheddar", "cheddar cheese");
        synonym("oil", "vegetable oil", "sunflower oil", "cooking oil", "canola oil");
        synonym("stock cube", "bouillon cube", "chicken stock cube", "beef stock cube", "vegetable stock cube");
        synonym("garlic", "garlic clove", "minced garlic");
        synonym("milk", "full cream milk", "low fat milk", "skim milk", "long life milk");
        synonym("rice", "white rice", "basmati rice", "jasmine rice", "long grain rice");
        synonym("oats", "oat", "rolled oats", "oatmeal");
        synonym("sugar", "white sugar", "caster sugar", "granulated sugar");
        synonym("pasta", "penne", "macaroni", "fusilli");
    }

    private IngredientNormalizer() {
    }

    private static void synonym(String canonical, String... others) {
        for (String o : others) {
            SYNONYMS.put(o, canonical);
        }
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        // 1. Lower-case, drop apostrophes, turn other punctuation into spaces.
        String cleaned = raw.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replaceAll("[^\\p{L}\\p{N} ]", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        List<String> words = new ArrayList<>(Arrays.asList(cleaned.split("\\s+")));

        // 2. "cloves of garlic" -> "garlic"
        if (words.size() > 2 && MEASURE_WORDS.contains(words.get(0)) && words.get(1).equals("of")) {
            words = new ArrayList<>(words.subList(2, words.size()));
        }

        // 3. Remove describing words, unless that would leave nothing.
        List<String> kept = new ArrayList<>();
        for (String w : words) {
            if (!DESCRIPTORS.contains(w)) {
                kept.add(w);
            }
        }
        if (kept.isEmpty()) {
            kept = words;
        }

        // 4. Singularise every word.
        List<String> singularWords = new ArrayList<>();
        for (String w : kept) {
            singularWords.add(singular(w));
        }
        String phrase = join(singularWords);

        // 5. Map known synonyms to one canonical name. Checked before and after singularising
        //    so that both "rolled oats" and "rolled oat" are recognised.
        String unsingularised = join(kept);
        if (SYNONYMS.containsKey(unsingularised)) {
            return SYNONYMS.get(unsingularised);
        }
        String mapped = SYNONYMS.get(phrase);
        return mapped != null ? mapped : phrase;
    }

    private static String join(List<String> words) {
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(w);
        }
        return sb.toString();
    }

    /** Rule-based English singulariser: good enough for ingredient names, not a full NLP. */
    static String singular(String w) {
        if (w.length() <= 3 || INVARIANT.contains(w)) {
            return w;
        }
        if (IRREGULAR.containsKey(w)) {
            return IRREGULAR.get(w);
        }
        if (w.endsWith("ies")) {
            return w.substring(0, w.length() - 3) + "y";          // berries -> berry
        }
        if (w.endsWith("oes")) {
            return w.substring(0, w.length() - 2);                // tomatoes -> tomato
        }
        if (w.endsWith("ches") || w.endsWith("shes") || w.endsWith("sses")
                || w.endsWith("xes") || w.endsWith("zes")) {
            return w.substring(0, w.length() - 2);                // peaches -> peach
        }
        if (w.endsWith("ss") || w.endsWith("us") || w.endsWith("is")) {
            return w;                                             // watercress, citrus
        }
        if (w.endsWith("s")) {
            return w.substring(0, w.length() - 1);                // eggs -> egg
        }
        return w;
    }
}
