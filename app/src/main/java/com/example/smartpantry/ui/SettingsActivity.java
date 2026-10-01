package com.example.smartpantry.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.smartpantry.R;
import com.example.smartpantry.data.DatabaseHelper;
import com.example.smartpantry.logic.UnitConverter;
import com.example.smartpantry.util.Prefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Settings screen. Every change is saved immediately. */
public class SettingsActivity extends AppCompatActivity {

    private static final Integer[] DAY_OPTIONS = {1, 2, 3, 5, 7};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);

        setUpExpiryAlerts();
        setUpAlmostThereToggle();
        setUpDefaultUnit();
        setUpClearPantryButton();

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        NavHelper.setup(this, nav, R.id.nav_settings);
    }

    private void setUpExpiryAlerts() {
        SwitchCompat switchExpiry = findViewById(R.id.switchExpiryAlerts);
        View rowExpiryDays = findViewById(R.id.rowExpiryDays);
        Spinner spDays = findViewById(R.id.spExpiryDays);

        boolean enabled = Prefs.isExpiryAlertsEnabled(this);
        switchExpiry.setChecked(enabled);
        rowExpiryDays.setEnabled(enabled);
        spDays.setEnabled(enabled);
        switchExpiry.setOnCheckedChangeListener((button, checked) -> {
            Prefs.setExpiryAlertsEnabled(this, checked);
            rowExpiryDays.setEnabled(checked);
            spDays.setEnabled(checked);
        });

        ArrayAdapter<Integer> daysAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, DAY_OPTIONS);
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDays.setAdapter(daysAdapter);
        int savedDays = Prefs.getExpiryDays(this);
        for (int i = 0; i < DAY_OPTIONS.length; i++) {
            if (DAY_OPTIONS[i] == savedDays) {
                spDays.setSelection(i);
                break;
            }
        }
        spDays.setOnItemSelectedListener(new SimpleSelection(pos -> Prefs.setExpiryDays(this, DAY_OPTIONS[pos])));
    }

    private void setUpAlmostThereToggle() {
        SwitchCompat switchAlmost = findViewById(R.id.switchAlmostThere);
        switchAlmost.setChecked(Prefs.isShowAlmostThere(this));
        switchAlmost.setOnCheckedChangeListener((button, checked) -> Prefs.setShowAlmostThere(this, checked));
    }

    /** The unit pre-selected when the user adds a new pantry item. */
    private void setUpDefaultUnit() {
        Spinner spUnit = findViewById(R.id.spDefaultUnit);
        String[] units = UnitConverter.symbols();
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, units);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);
        String savedUnit = Prefs.getDefaultUnit(this);
        for (int i = 0; i < units.length; i++) {
            if (units[i].equals(savedUnit)) {
                spUnit.setSelection(i);
                break;
            }
        }
        spUnit.setOnItemSelectedListener(new SimpleSelection(pos -> Prefs.setDefaultUnit(this, units[pos])));
    }

    private void setUpClearPantryButton() {
        Button btnClear = findViewById(R.id.btnClearPantry);
        btnClear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.clear_pantry_title)
                .setMessage(R.string.clear_pantry_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    int removed = DatabaseHelper.getInstance(this).deleteAllPantryItems();
                    Toast.makeText(this, getResources().getQuantityString(R.plurals.items_removed, removed, removed),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    /** Lets a spinner take a one-line lambda instead of a full OnItemSelectedListener. */
    private static class SimpleSelection implements AdapterView.OnItemSelectedListener {
        interface OnPick { void picked(int position); }

        private final OnPick onPick;

        SimpleSelection(OnPick onPick) {
            this.onPick = onPick;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            onPick.picked(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
}
