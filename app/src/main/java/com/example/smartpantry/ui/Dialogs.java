package com.example.smartpantry.ui;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.example.smartpantry.R;

/** Dialogs shared by more than one screen. */
final class Dialogs {

    private Dialogs() {
    }

    /** Asks the user to confirm deleting the named pantry item, then runs onConfirm. */
    static void confirmDelete(Context context, String itemName, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.delete_item_title)
                .setMessage(context.getString(R.string.delete_item_message, itemName))
                .setPositiveButton(R.string.delete, (dialog, which) -> onConfirm.run())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
