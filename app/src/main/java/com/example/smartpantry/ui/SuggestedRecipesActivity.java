package com.example.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.adapter.RecipeAdapter;
import com.example.smartpantry.data.DatabaseHelper;
import com.example.smartpantry.logic.RecipeMatcher;
import com.example.smartpantry.model.PantryItem;
import com.example.smartpantry.model.Recipe;
import com.example.smartpantry.util.Prefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/** Shows recipes the pantry fully covers, plus an optional separate "Almost There" section. */
public class SuggestedRecipesActivity extends AppCompatActivity implements RecipeAdapter.Listener {

    private DatabaseHelper dbHelper;
    private RecipeAdapter suggestedAdapter;
    private RecipeAdapter almostAdapter;
    private TextView tvSuggestedHeader;
    private TextView tvEmptySuggested;
    private TextView tvAlmostHeader;
    private TextView tvEmptyAlmost;
    private View almostSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);
        setTitle(R.string.title_suggestions);

        dbHelper = DatabaseHelper.getInstance(this);
        tvSuggestedHeader = findViewById(R.id.tvSuggestedHeader);
        tvEmptySuggested = findViewById(R.id.tvEmptySuggested);
        tvAlmostHeader = findViewById(R.id.tvAlmostHeader);
        tvEmptyAlmost = findViewById(R.id.tvEmptyAlmost);
        almostSection = findViewById(R.id.almostSection);

        suggestedAdapter = new RecipeAdapter(this);
        almostAdapter = new RecipeAdapter(this);
        setUpList(findViewById(R.id.rvSuggested), suggestedAdapter);
        setUpList(findViewById(R.id.rvAlmostThere), almostAdapter);

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        NavHelper.setup(this, nav, R.id.nav_suggestions);
    }

    private void setUpList(RecyclerView rv, RecipeAdapter adapter) {
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false); // the whole page scrolls, not each list
        rv.setAdapter(adapter);
    }

    /** Re-run matching every time the screen is shown, so pantry changes apply immediately. */
    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<Recipe> recipes = dbHelper.getAllRecipes();
        List<PantryItem> pantry = dbHelper.getAllPantryItems();
        RecipeMatcher.Result result = RecipeMatcher.match(recipes, pantry);

        List<RecipeAdapter.Row> strictRows = new ArrayList<>();
        for (Recipe r : result.suggested) {
            strictRows.add(new RecipeAdapter.Row(r, null));
        }
        suggestedAdapter.setRows(strictRows);
        tvSuggestedHeader.setText(getString(R.string.ready_to_cook_header, strictRows.size()));
        if (strictRows.isEmpty()) {
            tvEmptySuggested.setText(pantry.isEmpty() ? R.string.empty_suggestions_no_pantry : R.string.empty_suggestions);
            tvEmptySuggested.setVisibility(View.VISIBLE);
        } else {
            tvEmptySuggested.setVisibility(View.GONE);
        }

        if (!Prefs.isShowAlmostThere(this)) {
            almostSection.setVisibility(View.GONE);
            return;
        }
        almostSection.setVisibility(View.VISIBLE);
        List<RecipeAdapter.Row> almostRows = new ArrayList<>();
        for (RecipeMatcher.AlmostThere a : result.almostThere) {
            almostRows.add(new RecipeAdapter.Row(a.recipe, a.missing.describeShortfall()));
        }
        almostAdapter.setRows(almostRows);
        tvAlmostHeader.setText(getString(R.string.almost_there_header, almostRows.size()));
        tvEmptyAlmost.setVisibility(almostRows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }
}
