package com.example.smartpantry.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.logic.UnitConverter;
import com.example.smartpantry.model.PantryItem;
import com.example.smartpantry.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/** Shows pantry items, highlighting ones that are expired or expiring soon when alerts are on. */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {

    public interface Listener {
        void onItemClick(PantryItem item);

        void onDeleteClick(PantryItem item);
    }

    private final List<PantryItem> items = new ArrayList<>();
    private final Listener listener;
    private boolean alertsEnabled;
    private int alertDays;

    public PantryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<PantryItem> newItems, boolean alertsEnabled, int alertDays) {
        items.clear();
        items.addAll(newItems);
        this.alertsEnabled = alertsEnabled;
        this.alertDays = alertDays;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PantryItem item = items.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvName.setText(item.getName());
        holder.tvQuantity.setText(ctx.getString(R.string.quantity_with_unit, UnitConverter.format(item.getQuantity()), item.getUnit()));

        String expiry = item.getExpiryDate();
        holder.tvExpiry.setTextColor(holder.defaultExpiryColors);
        if (expiry == null) {
            holder.tvExpiry.setText(R.string.no_expiry);
        } else {
            holder.tvExpiry.setText(ctx.getString(R.string.expires_on, DateUtils.toDisplay(expiry)));
            Long days = DateUtils.daysUntil(expiry);
            if (alertsEnabled && days != null) {
                if (days < 0) {
                    holder.tvExpiry.setText(ctx.getString(R.string.expired_on, DateUtils.toDisplay(expiry)));
                    holder.tvExpiry.setTextColor(ContextCompat.getColor(ctx, R.color.danger));
                } else if (days == 0) {
                    holder.tvExpiry.setText(R.string.expires_today);
                    holder.tvExpiry.setTextColor(ContextCompat.getColor(ctx, R.color.warning));
                } else if (days <= alertDays) {
                    int d = days.intValue();
                    holder.tvExpiry.setText(ctx.getResources().getQuantityString(R.plurals.expires_in_days, d, d));
                    holder.tvExpiry.setTextColor(ContextCompat.getColor(ctx, R.color.warning));
                }
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvQuantity;
        final TextView tvExpiry;
        final ImageButton btnDelete;
        final ColorStateList defaultExpiryColors;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvItemName);
            tvQuantity = v.findViewById(R.id.tvItemQuantity);
            tvExpiry = v.findViewById(R.id.tvItemExpiry);
            btnDelete = v.findViewById(R.id.btnDeleteItem);
            defaultExpiryColors = tvExpiry.getTextColors();
        }
    }
}
