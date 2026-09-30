package com.example.smartpantry.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.smartpantry.model.PantryItem;
import com.example.smartpantry.model.Recipe;
import com.example.smartpantry.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tables:
 *   pantry_items       - the user's ingredients (editable)
 *   recipes            - recipes seeded on first run (read-only for the user)
 *   recipe_ingredients - the ingredients of each recipe (foreign key to recipes)
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "smart_pantry.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_PANTRY = "pantry_items";
    public static final String TABLE_RECIPES = "recipes";
    public static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";

    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_UNIT = "unit";
    public static final String COL_EXPIRY = "expiry_date";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_STEPS = "steps";
    public static final String COL_RECIPE_ID = "recipe_id";

    private static DatabaseHelper instance;

    /** One shared helper for the whole app, so only one connection is opened. */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PANTRY + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_QUANTITY + " REAL NOT NULL CHECK (" + COL_QUANTITY + " > 0), "
                + COL_UNIT + " TEXT NOT NULL, "
                + COL_EXPIRY + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL UNIQUE, "
                + COL_DESCRIPTION + " TEXT, "
                + COL_STEPS + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_RECIPE_ID + " INTEGER NOT NULL REFERENCES " + TABLE_RECIPES + "(" + COL_ID + ") ON DELETE CASCADE, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_QUANTITY + " REAL NOT NULL, "
                + COL_UNIT + " TEXT NOT NULL)");

        db.execSQL("CREATE INDEX idx_recipe_ingredients_recipe ON "
                + TABLE_RECIPE_INGREDIENTS + "(" + COL_RECIPE_ID + ")");

        seedRecipes(db);
    }

    /**
     * Pre-loads the recipe collection. onCreate only runs the first time the database file is
     * created, so the recipes are seeded exactly once. onCreate already runs inside a
     * transaction, so all inserts succeed or fail together.
     */
    private void seedRecipes(SQLiteDatabase db) {
        for (Recipe recipe : RecipeSeedData.getRecipes()) {
            ContentValues recipeValues = new ContentValues();
            recipeValues.put(COL_NAME, recipe.getName());
            recipeValues.put(COL_DESCRIPTION, recipe.getDescription());
            recipeValues.put(COL_STEPS, recipe.getSteps());
            long recipeId = db.insertOrThrow(TABLE_RECIPES, null, recipeValues);

            for (RecipeIngredient ing : recipe.getIngredients()) {
                ContentValues ingredientValues = new ContentValues();
                ingredientValues.put(COL_RECIPE_ID, recipeId);
                ingredientValues.put(COL_NAME, ing.getName());
                ingredientValues.put(COL_QUANTITY, ing.getQuantity());
                ingredientValues.put(COL_UNIT, ing.getUnit());
                db.insertOrThrow(TABLE_RECIPE_INGREDIENTS, null, ingredientValues);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Rebuilds everything on a schema change. This also wipes the user's pantry,
        // so replace it with real migrations before bumping DB_VERSION in a release.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANTRY);
        onCreate(db);
    }

    /** Returns the new row id, or -1 on failure. */
    public long insertPantryItem(PantryItem item) {
        return getWritableDatabase().insert(TABLE_PANTRY, null, toValues(item));
    }

    /** All pantry items, sorted by name. */
    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE_PANTRY, null, null, null, null, null,
                COL_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                items.add(cursorToPantryItem(c));
            }
        }
        return items;
    }

    /** Returns null if not found. */
    public PantryItem getPantryItem(long id) {
        try (Cursor c = getReadableDatabase().query(TABLE_PANTRY, null, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? cursorToPantryItem(c) : null;
        }
    }

    /** Returns the number of rows changed (1 on success). */
    public int updatePantryItem(PantryItem item) {
        return getWritableDatabase().update(TABLE_PANTRY, toValues(item), COL_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
    }

    /** Returns the number of rows removed (1 on success). */
    public int deletePantryItem(long id) {
        return getWritableDatabase().delete(TABLE_PANTRY, COL_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public int deleteAllPantryItems() {
        return getWritableDatabase().delete(TABLE_PANTRY, null, null);
    }

    private ContentValues toValues(PantryItem item) {
        ContentValues v = new ContentValues();
        v.put(COL_NAME, item.getName());
        v.put(COL_QUANTITY, item.getQuantity());
        v.put(COL_UNIT, item.getUnit());
        if (item.getExpiryDate() == null) {
            v.putNull(COL_EXPIRY);
        } else {
            v.put(COL_EXPIRY, item.getExpiryDate());
        }
        return v;
    }

    private PantryItem cursorToPantryItem(Cursor c) {
        PantryItem item = new PantryItem();
        item.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        item.setName(c.getString(c.getColumnIndexOrThrow(COL_NAME)));
        item.setQuantity(c.getDouble(c.getColumnIndexOrThrow(COL_QUANTITY)));
        item.setUnit(c.getString(c.getColumnIndexOrThrow(COL_UNIT)));
        int expiryCol = c.getColumnIndexOrThrow(COL_EXPIRY);
        item.setExpiryDate(c.isNull(expiryCol) ? null : c.getString(expiryCol));
        return item;
    }

    /** All recipes with their ingredients, loaded with two queries rather than one per recipe. */
    public List<Recipe> getAllRecipes() {
        SQLiteDatabase db = getReadableDatabase();
        Map<Long, Recipe> byId = new LinkedHashMap<>();

        try (Cursor c = db.query(TABLE_RECIPES, null, null, null, null, null, COL_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                Recipe r = cursorToRecipe(c);
                byId.put(r.getId(), r);
            }
        }
        try (Cursor c = db.query(TABLE_RECIPE_INGREDIENTS, null, null, null, null, null, COL_ID + " ASC")) {
            while (c.moveToNext()) {
                RecipeIngredient ing = cursorToIngredient(c);
                Recipe owner = byId.get(ing.getRecipeId());
                if (owner != null) {
                    owner.getIngredients().add(ing);
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    /** One recipe with its ingredients, or null if not found. */
    public Recipe getRecipe(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Recipe recipe;
        try (Cursor c = db.query(TABLE_RECIPES, null, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (!c.moveToFirst()) {
                return null;
            }
            recipe = cursorToRecipe(c);
        }
        try (Cursor c = db.query(TABLE_RECIPE_INGREDIENTS, null, COL_RECIPE_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, COL_ID + " ASC")) {
            while (c.moveToNext()) {
                recipe.getIngredients().add(cursorToIngredient(c));
            }
        }
        return recipe;
    }

    private Recipe cursorToRecipe(Cursor c) {
        Recipe r = new Recipe();
        r.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        r.setName(c.getString(c.getColumnIndexOrThrow(COL_NAME)));
        r.setDescription(c.getString(c.getColumnIndexOrThrow(COL_DESCRIPTION)));
        r.setSteps(c.getString(c.getColumnIndexOrThrow(COL_STEPS)));
        return r;
    }

    private RecipeIngredient cursorToIngredient(Cursor c) {
        RecipeIngredient ing = new RecipeIngredient();
        ing.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        ing.setRecipeId(c.getLong(c.getColumnIndexOrThrow(COL_RECIPE_ID)));
        ing.setName(c.getString(c.getColumnIndexOrThrow(COL_NAME)));
        ing.setQuantity(c.getDouble(c.getColumnIndexOrThrow(COL_QUANTITY)));
        ing.setUnit(c.getString(c.getColumnIndexOrThrow(COL_UNIT)));
        return ing;
    }
}
