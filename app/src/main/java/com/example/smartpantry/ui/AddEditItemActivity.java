package com.example.smartpantry.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.example.smartpantry.data.DatabaseHelper;
import com.example.smartpantry.logic.UnitConverter;
import com.example.smartpantry.model.PantryItem;
import com.example.smartpantry.util.DateUtils;
import com.example.smartpantry.util.Prefs;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

/**
 * Adds a new pantry item, or edits an existing one when the Intent carries EXTRA_ITEM_ID.
 * Delete is only available when editing.
 */
public class AddEditItemActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "com.example.smartpantry.EXTRA_ITEM_ID";

    private static final long NO_ID = -1;
    private static final String STATE_EXPIRY = "expiry";
    private static final int MAX_NAME_LENGTH = 40;
    private static final double MAX_QUANTITY = 100000;

    private DatabaseHelper dbHelper;
    private TextInputLayout tilName;
    private TextInputLayout tilQuantity;
    private TextInputEditText etName;
    private TextInputEditText etQuantity;
    private Spinner spUnit;
    private TextView tvExpiry;
    private TextView tvExpiryError;
    private String[] units;

    private long itemId = NO_ID;
    private String selectedExpiry; // "yyyy-MM-dd" or null

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = DatabaseHelper.getInstance(this);
        tilName = findViewById(R.id.tilName);
        tilQuantity = findViewById(R.id.tilQuantity);
        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        spUnit = findViewById(R.id.spUnit);
        tvExpiry = findViewById(R.id.tvExpiry);
        tvExpiryError = findViewById(R.id.tvExpiryError);
        Button btnClearExpiry = findViewById(R.id.btnClearExpiry);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnDelete = findViewById(R.id.btnDelete);

        units = UnitConverter.symbols();
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, units);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);

        itemId = getIntent().getLongExtra(EXTRA_ITEM_ID, NO_ID);
        if (isEditing()) {
            setTitle(R.string.title_edit_item);
            PantryItem item = dbHelper.getPantryItem(itemId);
            if (item == null) {
                Toast.makeText(this, R.string.item_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            // Only prefill on first launch; after rotation the fields restore what the user typed.
            if (savedInstanceState == null) {
                etName.setText(item.getName());
                etQuantity.setText(UnitConverter.format(item.getQuantity()));
                selectUnit(item.getUnit());
                selectedExpiry = item.getExpiryDate();
            }
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            setTitle(R.string.title_add_item);
            if (savedInstanceState == null) {
                selectUnit(Prefs.getDefaultUnit(this));
            }
            btnDelete.setVisibility(View.GONE);
        }
        if (savedInstanceState != null) {
            selectedExpiry = savedInstanceState.getString(STATE_EXPIRY);
        }
        updateExpiryLabel();

        etName.addTextChangedListener(clearErrorOnType(tilName));
        etQuantity.addTextChangedListener(clearErrorOnType(tilQuantity));
        tvExpiry.setOnClickListener(v -> showDatePicker());
        btnClearExpiry.setOnClickListener(v -> {
            selectedExpiry = null;
            updateExpiryLabel();
        });
        btnSave.setOnClickListener(v -> save());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_EXPIRY, selectedExpiry);
    }

    private boolean isEditing() {
        return itemId != NO_ID;
    }

    private void selectUnit(String unit) {
        UnitConverter.Unit parsed = UnitConverter.parse(unit);
        String symbol = parsed != null ? parsed.symbol : UnitConverter.Unit.G.symbol;
        for (int i = 0; i < units.length; i++) {
            if (units[i].equals(symbol)) {
                spUnit.setSelection(i);
                return;
            }
        }
    }

    private void showDatePicker() {
        Calendar current = selectedExpiry != null ? DateUtils.parse(selectedExpiry) : null;
        if (current == null) {
            current = Calendar.getInstance();
        }
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedExpiry = DateUtils.toStorage(year, month, day);
                    tvExpiryError.setVisibility(View.GONE);
                    updateExpiryLabel();
                },
                current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
        if (!isEditing()) {
            // New items cannot be given an expiry date in the past
            dialog.getDatePicker().setMinDate(DateUtils.startOfToday().getTimeInMillis());
        }
        dialog.show();
    }

    private void updateExpiryLabel() {
        tvExpiry.setText(selectedExpiry == null
                ? getString(R.string.tap_to_set_expiry)
                : DateUtils.toDisplay(selectedExpiry));
    }

    /** Checks every field and shows all errors at once, so the user can fix them in one pass. */
    private boolean validate() {
        boolean valid = true;

        String name = cleanName();
        if (name.isEmpty()) {
            tilName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (name.length() > MAX_NAME_LENGTH) {
            tilName.setError(getString(R.string.error_name_too_long, MAX_NAME_LENGTH));
            valid = false;
        } else if (!name.matches("[\\p{L}\\p{N} '\\-]+") || !name.matches(".*\\p{L}.*")) {
            tilName.setError(getString(R.string.error_name_invalid));
            valid = false;
        }

        String rawQty = quantityText();
        if (rawQty.isEmpty()) {
            tilQuantity.setError(getString(R.string.error_quantity_required));
            valid = false;
        } else {
            try {
                double qty = Double.parseDouble(rawQty);
                if (Double.isNaN(qty) || Double.isInfinite(qty)) {
                    tilQuantity.setError(getString(R.string.error_quantity_invalid));
                    valid = false;
                } else if (qty <= 0) {
                    tilQuantity.setError(getString(R.string.error_quantity_positive));
                    valid = false;
                } else if (qty > MAX_QUANTITY) {
                    tilQuantity.setError(getString(R.string.error_quantity_too_large));
                    valid = false;
                }
            } catch (NumberFormatException e) {
                tilQuantity.setError(getString(R.string.error_quantity_invalid));
                valid = false;
            }
        }

        if (selectedExpiry != null && !isEditing()) {
            Long days = DateUtils.daysUntil(selectedExpiry);
            if (days == null || days < 0) {
                tvExpiryError.setVisibility(View.VISIBLE);
                valid = false;
            }
        }
        return valid;
    }

    /** Trimmed name with runs of whitespace collapsed to a single space. */
    private String cleanName() {
        return textOf(etName).replaceAll("\\s+", " ");
    }

    /** Quantity text with a comma decimal separator (common on South African keyboards) turned into a dot. */
    private String quantityText() {
        return textOf(etQuantity).replace(',', '.');
    }

    private void save() {
        if (!validate()) {
            return;
        }
        PantryItem item = new PantryItem(
                cleanName(),
                Double.parseDouble(quantityText()),
                (String) spUnit.getSelectedItem(),
                selectedExpiry);

        if (isEditing()) {
            item.setId(itemId);
            if (dbHelper.updatePantryItem(item) == 0) {
                Toast.makeText(this, R.string.error_saving, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, getString(R.string.item_updated, item.getName()), Toast.LENGTH_SHORT).show();
        } else {
            if (dbHelper.insertPantryItem(item) == NO_ID) {
                Toast.makeText(this, R.string.error_saving, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, getString(R.string.item_added, item.getName()), Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void confirmDelete() {
        Dialogs.confirmDelete(this, textOf(etName), () -> {
            if (dbHelper.deletePantryItem(itemId) == 0) {
                Toast.makeText(this, R.string.error_deleting, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, R.string.item_deleted_generic, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private static TextWatcher clearErrorOnType(TextInputLayout layout) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { layout.setError(null); }
            @Override public void afterTextChanged(Editable s) { }
        };
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
