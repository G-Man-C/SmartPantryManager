package com.example.smartpantry.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Expiry dates are stored as "yyyy-MM-dd" text in SQLite (sortable and unambiguous) and shown
 * to the user as "dd MMM yyyy". Uses Calendar rather than java.time so it works on API 24.
 */
public final class DateUtils {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private DateUtils() {
    }

    private static SimpleDateFormat storageFormat() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setLenient(false);
        return f;
    }

    /** month0 is 0-based, as returned by DatePickerDialog. */
    public static String toStorage(int year, int month0, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month0 + 1, day);
    }

    public static Calendar parse(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(storageFormat().parse(stored));
            return c;
        } catch (ParseException e) {
            return null;
        }
    }

    public static String toDisplay(String stored) {
        Calendar c = parse(stored);
        if (c == null) {
            return stored == null ? "" : stored;
        }
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(c.getTime());
    }

    public static Calendar startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /** Days from today until the date (negative = already expired). Null if no/invalid date. */
    public static Long daysUntil(String stored) {
        Calendar target = parse(stored);
        if (target == null) {
            return null;
        }
        long diff = target.getTimeInMillis() - startOfToday().getTimeInMillis();
        return Math.round(diff / (double) DAY_MS); // round() absorbs daylight-saving shifts
    }
}
