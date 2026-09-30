package com.example.smartpantry.model;

import java.util.ArrayList;
import java.util.List;

/** A recipe: name, short description, preparation steps and its required ingredients. */
public class Recipe {

    private long id;
    private String name;
    private String description;
    /** Steps separated by newline characters, one step per line. */
    private String steps;
    private final List<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe() {
    }

    public Recipe(String name, String description, String steps) {
        this.name = name;
        this.description = description;
        this.steps = steps;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }

    public List<RecipeIngredient> getIngredients() { return ingredients; }
}
