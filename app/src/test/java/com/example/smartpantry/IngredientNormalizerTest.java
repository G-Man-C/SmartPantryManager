package com.example.smartpantry;

import static org.junit.Assert.assertEquals;

import com.example.smartpantry.logic.IngredientNormalizer;

import org.junit.Test;

public class IngredientNormalizerTest {

    private static void same(String a, String b) {
        assertEquals(a + " vs " + b, IngredientNormalizer.normalize(a), IngredientNormalizer.normalize(b));
    }

    @Test
    public void pluralsBecomeSingular() {
        same("tomato", "Tomatoes");
        same("potato", "potatoes");
        same("egg", "EGGS");
        same("berry", "berries");
        same("peach", "peaches");
        same("olive", "olives");
    }

    @Test
    public void descriptorsAndPunctuationAreIgnored() {
        same("egg", "  Fresh  large eggs ");
        same("olive oil", "Extra-virgin olive oil");
        same("garlic", "cloves of garlic");
    }

    @Test
    public void wordsEndingInSThatAreSingularAreKept() {
        assertEquals("couscous", IngredientNormalizer.normalize("Couscous"));
        assertEquals("hummus", IngredientNormalizer.normalize("hummus"));
        assertEquals("oats", IngredientNormalizer.normalize("Rolled oats"));
    }

    @Test
    public void synonymsMapToOneName() {
        same("spring onion", "scallions");
        same("flour", "Cake flour");
        same("maize meal", "Mielie meal");
        same("zucchini", "baby marrow");
        same("oil", "Sunflower oil");
    }
}
