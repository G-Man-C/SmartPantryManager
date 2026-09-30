package com.example.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.adapter.PantryAdapter;
import com.example.smartpantry.data.DatabaseHelper;
import com.example.smartpantry.model.PantryItem;
import com.example.smartpantry.util.DateUtils;
import com.example.smartpantry.util.Prefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/** Launcher screen: lists every pantry item and offers add, edit and delete. */
public class PantryListActivity extends AppCompatActivity implements PantryAdapter.Listener {

    private DatabaseHelper dbHelper;
    private PantryAdapter adapter;
    private TextView tvEmpty;
    private TextView tvExpiryBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry_list);
        setTitle(R.string.title_pantry);

        dbHelper = DatabaseHelper.getInstance(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvExpiryBanner = findViewById(R.id.tvExpiryBanner);

        RecyclerView rv = findViewById(R.id.rvPantry);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryAdapter(this);
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddEditItemActivity.class)));

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        NavHelper.setup(this, nav, R.id.nav_pantry);
    }

    /** Reload on every return to this screen so adds, edits and setting changes show up. */
    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    private void loadItems() {
        List<PantryItem> items = dbHelper.getAllPantryItems();
        boolean alerts = Prefs.isExpiryAlertsEnabled(this);
        int days = Prefs.getExpiryDays(this);
        adapter.setData(items, alerts, days);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        updateExpiryBanner(items, alerts, days);
    }

    private void updateExpiryBanner(List<PantryItem> items, boolean alerts, int days) {
        if (!alerts) {
            tvExpiryBanner.setVisibility(View.GONE);
            return;
        }
        int count = 0;
        for (PantryItem item : items) {
            Long d = DateUtils.daysUntil(item.getExpiryDate());
            if (d != null && d <= days) {
                count++;
            }
        }
        if (count == 0) {
            tvExpiryBanner.setVisibility(View.GONE);
        } else {
            tvExpiryBanner.setText(getResources().getQuantityString(R.plurals.expiry_banner, count, count, days));
            tvExpiryBanner.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(PantryItem item) {
        Intent intent = new Intent(this, AddEditItemActivity.class);
        intent.putExtra(AddEditItemActivity.EXTRA_ITEM_ID, item.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(PantryItem item) {
        Dialogs.confirmDelete(this, item.getName(), () -> {
            if (dbHelper.deletePantryItem(item.getId()) > 0) {
                Toast.makeText(this, getString(R.string.item_deleted, item.getName()), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.error_deleting, Toast.LENGTH_SHORT).show();
            }
            loadItems();
        });
    }
}
