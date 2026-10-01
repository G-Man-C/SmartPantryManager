package com.example.smartpantry.ui;

import android.app.Activity;
import android.content.Intent;

import com.example.smartpantry.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Shared bottom navigation for the three top-level screens. Each tab opens its Activity with an
 * explicit Intent. REORDER_TO_FRONT reuses an existing screen instead of stacking duplicates.
 */
public final class NavHelper {

    private NavHelper() {
    }

    public static void setup(final Activity activity, BottomNavigationView nav, final int currentItemId) {
        nav.setSelectedItemId(currentItemId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentItemId) {
                return true;
            }
            Class<?> target;
            if (id == R.id.nav_pantry) {
                target = PantryListActivity.class;
            } else if (id == R.id.nav_suggestions) {
                target = SuggestedRecipesActivity.class;
            } else if (id == R.id.nav_settings) {
                target = SettingsActivity.class;
            } else {
                return false;
            }
            Intent intent = new Intent(activity, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            // false: keep this screen's own tab highlighted for when the user comes back to it
            return false;
        });
    }
}
