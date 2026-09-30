package com.example.smartpantry.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.smartpantry.logic.UnitConverter;

/** User settings, stored in SharedPreferences. */
public final class Prefs {

    private static final String FILE = "smart_pantry_settings";
    private static final String KEY_EXPIRY_ALERTS = "expiry_alerts";
    private static final String KEY_EXPIRY_DAYS = "expiry_days";
    private static final String KEY_SHOW_ALMOST_THERE = "show_almost_there";
    private static final String KEY_DEFAULT_UNIT = "default_unit";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isExpiryAlertsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_EXPIRY_ALERTS, true);
    }

    public static void setExpiryAlertsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_EXPIRY_ALERTS, value).apply();
    }

    public static int getExpiryDays(Context context) {
        return prefs(context).getInt(KEY_EXPIRY_DAYS, 3);
    }

    public static void setExpiryDays(Context context, int days) {
        prefs(context).edit().putInt(KEY_EXPIRY_DAYS, days).apply();
    }

    public static boolean isShowAlmostThere(Context context) {
        return prefs(context).getBoolean(KEY_SHOW_ALMOST_THERE, true);
    }

    public static void setShowAlmostThere(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_SHOW_ALMOST_THERE, value).apply();
    }

    public static String getDefaultUnit(Context context) {
        return prefs(context).getString(KEY_DEFAULT_UNIT, UnitConverter.Unit.G.symbol);
    }

    public static void setDefaultUnit(Context context, String unit) {
        prefs(context).edit().putString(KEY_DEFAULT_UNIT, unit).apply();
    }
}
