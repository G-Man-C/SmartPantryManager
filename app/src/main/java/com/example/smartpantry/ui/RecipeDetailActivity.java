package com.example.smartpantry.ui;

import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smartpantry.R;
import com.example.smartpantry.data.DatabaseHelper;
import com.example.smartpantry.logic.RecipeMatcher;
import com.example.smartpantry.logic.UnitConverter;
import com.example.smartpantry.model.Recipe;
import com.example.smartpantry.model.RecipeIngredient;

import java.util.List;

/** Full ingredient list (with have / missing status) and method for one recipe. */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "com.example.smartpantry.EXTRA_RECIPE_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        Recipe recipe = dbHelper.getRecipe(recipeId);
        if (recipe == null) {
            Toast.makeText(this, R.string.recipe_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setTitle(recipe.getName());

        TextView tvName = findViewById(R.id.tvDetailName);
        TextView tvDescription = findViewById(R.id.tvDetailDescription);
        TextView tvSteps = findViewById(R.id.tvSteps);
        tvName.setText(recipe.getName());
        tvDescription.setText(recipe.getDescription());
        tvSteps.setText(numberSteps(recipe.getSteps()));

        // Same matching logic as the suggestions screen, so the two screens always agree
        RecipeMatcher.PantryIndex index = RecipeMatcher.buildIndex(dbHelper.getAllPantryItems());
        List<RecipeMatcher.Requirement> requirements = RecipeMatcher.evaluate(recipe, index);
        int missing = showIngredients(requirements);
        showStatus(missing);
    }

    /** Adds one line per ingredient, coloured by whether the pantry covers it. Returns how many are missing. */
    private int showIngredients(List<RecipeMatcher.Requirement> requirements) {
        LinearLayout ingredientsContainer = findViewById(R.id.ingredientsContainer);
        int haveColor = ContextCompat.getColor(this, R.color.success);
        int missingColor = ContextCompat.getColor(this, R.color.danger);
        int verticalPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

        int missing = 0;
        for (RecipeMatcher.Requirement req : requirements) {
            boolean satisfied = req.isSatisfied();
            if (!satisfied) {
                missing++;
            }
            for (RecipeIngredient ing : req.getSources()) {
                String line = (satisfied ? "✓  " : "✗  ")
                        + getString(R.string.quantity_with_unit, UnitConverter.format(ing.getQuantity()), ing.getUnit())
                        + "  " + ing.getName();
                if (!satisfied) {
                    line += "\n     " + req.describeShortfall();
                }
                TextView tv = new TextView(this);
                tv.setText(line);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tv.setTextColor(satisfied ? haveColor : missingColor);
                tv.setPadding(0, verticalPadding, 0, verticalPadding);
                ingredientsContainer.addView(tv);
            }
        }
        return missing;
    }

    private void showStatus(int missing) {
        TextView tvStatus = findViewById(R.id.tvMatchStatus);
        if (missing == 0) {
            tvStatus.setText(R.string.status_can_cook);
            tvStatus.setBackgroundResource(R.color.success_bg);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            tvStatus.setText(getResources().getQuantityString(R.plurals.status_missing, missing, missing));
            tvStatus.setBackgroundResource(R.color.danger_bg);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.danger));
        }
    }

    /** Turns newline-separated steps into "1. ...", "2. ..." with a blank line between them. */
    private static String numberSteps(String steps) {
        StringBuilder numbered = new StringBuilder();
        String[] lines = steps.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                numbered.append("\n\n");
            }
            numbered.append(i + 1).append(". ").append(lines[i].trim());
        }
        return numbered.toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
