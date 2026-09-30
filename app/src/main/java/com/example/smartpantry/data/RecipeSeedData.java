package com.example.smartpantry.data;

import com.example.smartpantry.model.Recipe;
import com.example.smartpantry.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The 20 recipes pre-loaded into the database on first run.
 * Kept separate from DatabaseHelper so it is easy to edit and can be unit-tested.
 * Units used: g, kg, ml, l, tsp, tbsp, cup, pcs.
 */
public final class RecipeSeedData {

    private RecipeSeedData() {
    }

    private static RecipeIngredient ing(String name, double quantity, String unit) {
        return new RecipeIngredient(name, quantity, unit);
    }

    private static Recipe recipe(String name, String description,
                                 List<RecipeIngredient> ingredients, String... steps) {
        StringBuilder sb = new StringBuilder();
        for (String step : steps) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(step);
        }
        Recipe r = new Recipe(name, description, sb.toString());
        r.getIngredients().addAll(ingredients);
        return r;
    }

    public static List<Recipe> getRecipes() {
        List<Recipe> list = new ArrayList<>();

        list.add(recipe("Scrambled Eggs", "Soft, creamy eggs ready in five minutes.",
                Arrays.asList(ing("Eggs", 3, "pcs"), ing("Milk", 50, "ml"),
                        ing("Butter", 10, "g"), ing("Salt", 2, "g")),
                "Whisk the eggs, milk and salt together in a bowl.",
                "Melt the butter in a non-stick pan over low heat.",
                "Pour in the eggs and stir gently until just set.",
                "Serve immediately."));

        list.add(recipe("Cheese Omelette", "A quick, filling omelette with melted cheese.",
                Arrays.asList(ing("Eggs", 3, "pcs"), ing("Cheese", 40, "g"),
                        ing("Butter", 10, "g"), ing("Salt", 2, "g")),
                "Beat the eggs with the salt.",
                "Melt the butter in a pan over medium heat and pour in the eggs.",
                "When the edges set, sprinkle grated cheese over one half.",
                "Fold the omelette over and cook for another minute."));

        list.add(recipe("French Toast", "Golden egg-soaked bread for breakfast.",
                Arrays.asList(ing("Bread", 4, "pcs"), ing("Eggs", 2, "pcs"), ing("Milk", 100, "ml"),
                        ing("Sugar", 10, "g"), ing("Butter", 15, "g")),
                "Whisk the eggs, milk and sugar in a shallow dish.",
                "Dip each slice of bread in the mixture on both sides.",
                "Fry in butter over medium heat until golden on both sides."));

        list.add(recipe("Pancakes", "Classic fluffy pancakes.",
                Arrays.asList(ing("Flour", 200, "g"), ing("Eggs", 2, "pcs"), ing("Milk", 300, "ml"),
                        ing("Sugar", 25, "g"), ing("Butter", 20, "g")),
                "Mix the flour and sugar in a bowl.",
                "Whisk in the eggs and milk until smooth.",
                "Melt a little butter in a pan and pour in a ladle of batter.",
                "Flip when bubbles appear and cook until golden. Repeat."));

        list.add(recipe("Cheese Toastie", "Crispy toasted cheese sandwich.",
                Arrays.asList(ing("Bread", 2, "pcs"), ing("Cheese", 50, "g"), ing("Butter", 10, "g")),
                "Butter the outside of both slices of bread.",
                "Place the cheese between the slices, buttered sides out.",
                "Toast in a pan for 2-3 minutes per side until golden and melted."));

        list.add(recipe("Banana Smoothie", "A thick, sweet smoothie.",
                Arrays.asList(ing("Bananas", 2, "pcs"), ing("Milk", 250, "ml"), ing("Honey", 15, "ml")),
                "Peel and slice the bananas.",
                "Blend the bananas, milk and honey until smooth.",
                "Serve cold."));

        list.add(recipe("Oat Porridge with Banana", "Warm oats topped with banana and honey.",
                Arrays.asList(ing("Oats", 80, "g"), ing("Milk", 250, "ml"),
                        ing("Banana", 1, "pcs"), ing("Honey", 10, "ml")),
                "Bring the milk to a gentle simmer in a pot.",
                "Stir in the oats and cook for 5 minutes, stirring often.",
                "Top with sliced banana and a drizzle of honey."));

        list.add(recipe("Fruit Salad", "Fresh mixed fruit with a honey drizzle.",
                Arrays.asList(ing("Apple", 1, "pcs"), ing("Bananas", 2, "pcs"),
                        ing("Orange", 1, "pcs"), ing("Honey", 15, "ml")),
                "Chop the apple and bananas into bite-sized pieces.",
                "Peel and segment the orange.",
                "Toss everything together with the honey and serve."));

        list.add(recipe("Tomato Pasta", "Simple pasta in a fresh tomato and garlic sauce.",
                Arrays.asList(ing("Pasta", 200, "g"), ing("Tomatoes", 4, "pcs"), ing("Onion", 1, "pcs"),
                        ing("Garlic", 2, "pcs"), ing("Olive oil", 30, "ml"), ing("Salt", 3, "g")),
                "Cook the pasta in salted boiling water until al dente, then drain.",
                "Fry the chopped onion and garlic in olive oil until soft.",
                "Add the chopped tomatoes and simmer for 10 minutes.",
                "Toss the pasta through the sauce and serve."));

        list.add(recipe("Spaghetti Bolognese", "Rich meat sauce over spaghetti.",
                Arrays.asList(ing("Spaghetti", 250, "g"), ing("Beef mince", 400, "g"),
                        ing("Onion", 1, "pcs"), ing("Garlic", 2, "pcs"), ing("Tomatoes", 4, "pcs"),
                        ing("Oil", 20, "ml"), ing("Salt", 3, "g")),
                "Fry the onion and garlic in oil until soft.",
                "Add the mince and brown it well.",
                "Add the chopped tomatoes and salt, then simmer for 20 minutes.",
                "Cook the spaghetti, drain, and serve with the sauce."));

        list.add(recipe("Garlic Butter Rice", "Fragrant buttery rice side dish.",
                Arrays.asList(ing("Rice", 200, "g"), ing("Butter", 30, "g"),
                        ing("Garlic", 3, "pcs"), ing("Salt", 3, "g")),
                "Melt the butter in a pot and fry the chopped garlic for 1 minute.",
                "Add the rice and stir to coat.",
                "Add 400 ml water and the salt, cover and simmer for 15 minutes."));

        list.add(recipe("Egg Fried Rice", "A great way to use leftover rice.",
                Arrays.asList(ing("Rice", 250, "g"), ing("Eggs", 2, "pcs"), ing("Spring onions", 2, "pcs"),
                        ing("Soy sauce", 20, "ml"), ing("Oil", 20, "ml")),
                "Cook the rice and let it cool (or use leftover rice).",
                "Scramble the eggs in hot oil, then push them to the side.",
                "Add the rice and stir-fry for 3 minutes.",
                "Stir in the soy sauce and sliced spring onions."));

        list.add(recipe("Vegetable Stir-Fry", "Crunchy vegetables with rice.",
                Arrays.asList(ing("Rice", 200, "g"), ing("Carrots", 2, "pcs"), ing("Bell pepper", 1, "pcs"),
                        ing("Onion", 1, "pcs"), ing("Soy sauce", 30, "ml"), ing("Oil", 30, "ml")),
                "Cook the rice.",
                "Slice the carrots, pepper and onion into thin strips.",
                "Stir-fry the vegetables in hot oil for 5 minutes.",
                "Add the soy sauce and serve over the rice."));

        list.add(recipe("Chicken Stir-Fry", "Quick chicken with peppers and soy.",
                Arrays.asList(ing("Chicken breast", 300, "g"), ing("Bell pepper", 1, "pcs"),
                        ing("Onion", 1, "pcs"), ing("Garlic", 2, "pcs"),
                        ing("Soy sauce", 30, "ml"), ing("Oil", 20, "ml")),
                "Slice the chicken into strips.",
                "Stir-fry the chicken in hot oil until cooked through.",
                "Add the sliced pepper, onion and garlic and cook for 3 minutes.",
                "Stir in the soy sauce and serve."));

        list.add(recipe("Chicken Curry", "Mild, comforting curry served with rice.",
                Arrays.asList(ing("Chicken breast", 500, "g"), ing("Onion", 1, "pcs"),
                        ing("Tomatoes", 2, "pcs"), ing("Garlic", 3, "pcs"),
                        ing("Curry powder", 15, "g"), ing("Oil", 30, "ml"), ing("Rice", 250, "g")),
                "Fry the onion and garlic in oil until golden.",
                "Add the curry powder and cook for 1 minute.",
                "Add the cubed chicken and chopped tomatoes; simmer for 25 minutes.",
                "Cook the rice and serve the curry on top."));

        list.add(recipe("Potato Wedges", "Crispy oven-baked wedges.",
                Arrays.asList(ing("Potatoes", 4, "pcs"), ing("Oil", 30, "ml"),
                        ing("Salt", 3, "g"), ing("Paprika", 5, "g")),
                "Preheat the oven to 200 °C.",
                "Cut the potatoes into wedges and toss with oil, salt and paprika.",
                "Bake for 35-40 minutes, turning halfway."));

        list.add(recipe("Mashed Potatoes", "Smooth, buttery mash.",
                Arrays.asList(ing("Potatoes", 5, "pcs"), ing("Butter", 40, "g"),
                        ing("Milk", 100, "ml"), ing("Salt", 3, "g")),
                "Peel and boil the potatoes for 20 minutes until soft.",
                "Drain, then mash with the butter and warm milk.",
                "Season with salt and serve."));

        list.add(recipe("Greek Salad", "Fresh salad with feta and olive oil.",
                Arrays.asList(ing("Tomatoes", 3, "pcs"), ing("Cucumber", 1, "pcs"), ing("Onion", 1, "pcs"),
                        ing("Feta cheese", 100, "g"), ing("Olive oil", 30, "ml")),
                "Chop the tomatoes, cucumber and onion.",
                "Crumble the feta over the vegetables.",
                "Drizzle with olive oil and serve."));

        list.add(recipe("Pap with Tomato Relish", "Stiff pap served with a warm tomato and onion relish.",
                Arrays.asList(ing("Maize meal", 250, "g"), ing("Tomatoes", 3, "pcs"),
                        ing("Onion", 1, "pcs"), ing("Oil", 20, "ml"), ing("Salt", 5, "g")),
                "Bring 750 ml salted water to the boil.",
                "Slowly stir in the maize meal, cover and cook on low for 20 minutes, stirring occasionally.",
                "Meanwhile, fry the onion in oil, add the chopped tomatoes and simmer for 10 minutes.",
                "Serve the pap with the relish on top."));

        list.add(recipe("Vegetable Soup", "Hearty soup from everyday vegetables.",
                Arrays.asList(ing("Carrots", 2, "pcs"), ing("Potatoes", 2, "pcs"), ing("Onion", 1, "pcs"),
                        ing("Celery", 2, "pcs"), ing("Stock cube", 1, "pcs"), ing("Salt", 3, "g")),
                "Chop all the vegetables.",
                "Place in a pot with 1 litre of water and the crumbled stock cube.",
                "Simmer for 25 minutes until tender.",
                "Season with salt and blend if you prefer a smooth soup."));

        return list;
    }
}
