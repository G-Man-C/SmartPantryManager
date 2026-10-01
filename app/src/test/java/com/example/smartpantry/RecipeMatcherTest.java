package com.example.smartpantry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.smartpantry.data.RecipeSeedData;
import com.example.smartpantry.logic.RecipeMatcher;
import com.example.smartpantry.logic.UnitConverter;
import com.example.smartpantry.model.PantryItem;
import com.example.smartpantry.model.Recipe;
import com.example.smartpantry.model.RecipeIngredient;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the strict-matching rule (brief section 2.3).
 * Run in Android Studio: right-click this file > Run 'RecipeMatcherTest'. No emulator needed.
 */
public class RecipeMatcherTest {

    // ---- helpers ------------------------------------------------------------------------

    private static PantryItem item(String name, double qty, String unit) {
        return new PantryItem(name, qty, unit, null);
    }

    private static RecipeIngredient ing(String name, double qty, String unit) {
        return new RecipeIngredient(name, qty, unit);
    }

    private static Recipe recipe(String name, RecipeIngredient... ingredients) {
        Recipe r = new Recipe(name, "", "Step");
        r.getIngredients().addAll(Arrays.asList(ingredients));
        return r;
    }

    private static RecipeMatcher.Result match(Recipe recipe, PantryItem... pantry) {
        return RecipeMatcher.match(Collections.singletonList(recipe), Arrays.asList(pantry));
    }

    /** A 5-ingredient recipe used by several tests. */
    private static Recipe fiveIngredientRecipe() {
        return recipe("Pancakes",
                ing("Flour", 200, "g"), ing("Eggs", 2, "pcs"), ing("Milk", 300, "ml"),
                ing("Sugar", 25, "g"), ing("Butter", 20, "g"));
    }

    // ---- the strict rule ----------------------------------------------------------------

    @Test
    public void suggestedWhenEveryIngredientIsPresentInFull() {
        RecipeMatcher.Result r = match(fiveIngredientRecipe(),
                item("Flour", 500, "g"), item("Eggs", 6, "pcs"), item("Milk", 1, "l"),
                item("Sugar", 100, "g"), item("Butter", 250, "g"));
        assertEquals(1, r.suggested.size());
        assertEquals(0, r.almostThere.size());
    }

    @Test
    public void fourOfFiveIngredientsIsNotSuggested() {
        // The exact scenario from the brief: 5 needed, 4 present -> must NOT be suggested
        RecipeMatcher.Result r = match(fiveIngredientRecipe(),
                item("Flour", 500, "g"), item("Eggs", 6, "pcs"), item("Milk", 1, "l"),
                item("Sugar", 100, "g"));
        assertEquals(0, r.suggested.size());
        assertEquals("Belongs in the separate Almost There list", 1, r.almostThere.size());
        assertEquals("Butter", r.almostThere.get(0).missing.getDisplayName());
    }

    @Test
    public void notEnoughOfAnIngredientCountsAsMissing() {
        // Has butter, but 10 g instead of the 20 g needed
        RecipeMatcher.Result r = match(fiveIngredientRecipe(),
                item("Flour", 500, "g"), item("Eggs", 6, "pcs"), item("Milk", 1, "l"),
                item("Sugar", 100, "g"), item("Butter", 10, "g"));
        assertEquals(0, r.suggested.size());
        assertEquals(1, r.almostThere.size());
        assertEquals(10, r.almostThere.get(0).missing.getShortfallBase(), 1e-9);
    }

    @Test
    public void exactlyTheRequiredQuantityIsEnough() {
        RecipeMatcher.Result r = match(fiveIngredientRecipe(),
                item("Flour", 200, "g"), item("Eggs", 2, "pcs"), item("Milk", 300, "ml"),
                item("Sugar", 25, "g"), item("Butter", 20, "g"));
        assertEquals(1, r.suggested.size());
    }

    @Test
    public void twoMissingIngredientsAppearInNeitherList() {
        RecipeMatcher.Result r = match(fiveIngredientRecipe(),
                item("Flour", 500, "g"), item("Eggs", 6, "pcs"), item("Milk", 1, "l"));
        assertEquals(0, r.suggested.size());
        assertEquals(0, r.almostThere.size());
    }

    @Test
    public void emptyPantrySuggestsNothing() {
        RecipeMatcher.Result r = RecipeMatcher.match(RecipeSeedData.getRecipes(), new ArrayList<PantryItem>());
        assertEquals(0, r.suggested.size());
    }

    // ---- robustness to real-world messiness ---------------------------------------------

    @Test
    public void pluralsAndCapitalisationStillMatch() {
        Recipe salad = recipe("Salad", ing("tomato", 2, "pcs"), ing("Cucumbers", 1, "pcs"));
        RecipeMatcher.Result r = match(salad, item("  TOMATOES ", 3, "pcs"), item("cucumber", 1, "pcs"));
        assertEquals(1, r.suggested.size());
    }

    @Test
    public void unitsAreConvertedWithinTheSameCategory() {
        Recipe r1 = recipe("Mass", ing("Flour", 500, "g"));
        assertEquals(1, match(r1, item("Flour", 1, "kg")).suggested.size());

        Recipe r2 = recipe("Volume", ing("Milk", 250, "ml"));
        assertEquals(1, match(r2, item("Milk", 1, "cup")).suggested.size());

        Recipe r3 = recipe("Spoons", ing("Oil", 2, "tbsp"));
        assertEquals(1, match(r3, item("Oil", 30, "ml")).suggested.size());
        assertEquals(0, match(r3, item("Oil", 5, "tsp")).suggested.size()); // 25 ml < 30 ml
    }

    @Test
    public void differentUnitCategoriesNeverMatch() {
        // 2 cups of flour cannot be compared with grams without guessing a density
        Recipe r = recipe("Bread", ing("Flour", 200, "g"));
        assertEquals(0, match(r, item("Flour", 2, "cup")).suggested.size());
    }

    @Test
    public void severalPantryEntriesAreAddedTogether() {
        Recipe r = recipe("Omelette", ing("Eggs", 3, "pcs"));
        assertEquals(1, match(r, item("Egg", 1, "pcs"), item("eggs", 2, "pcs")).suggested.size());
    }

    @Test
    public void synonymsMatch() {
        Recipe r = recipe("Fried rice", ing("Spring onions", 2, "pcs"), ing("Rice", 200, "g"));
        RecipeMatcher.Result res = match(r, item("Scallions", 2, "pcs"), item("Basmati rice", 1, "kg"));
        assertEquals(1, res.suggested.size());
    }

    @Test
    public void differentIngredientsDoNotMatchByAccident() {
        Recipe r = recipe("Greek salad", ing("Feta cheese", 100, "g"));
        assertEquals(0, match(r, item("Cheese", 500, "g")).suggested.size());
    }

    // ---- seed data sanity -----------------------------------------------------------------

    @Test
    public void seedDataHasAtLeast20ValidRecipes() {
        List<Recipe> recipes = RecipeSeedData.getRecipes();
        assertTrue(recipes.size() >= 20);
        for (Recipe r : recipes) {
            assertFalse(r.getName() + " has no ingredients", r.getIngredients().isEmpty());
            assertFalse(r.getName() + " has no steps", r.getSteps().trim().isEmpty());
            for (RecipeIngredient i : r.getIngredients()) {
                assertTrue(r.getName() + ": unknown unit " + i.getUnit(), UnitConverter.parse(i.getUnit()) != null);
                assertTrue(i.getQuantity() > 0);
            }
        }
    }
}
