package com.example.smartpantry.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.model.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom adapter used by both recipe lists. For "Almost There" rows a note explains what is
 * missing; for strict suggestions the note is null and hidden.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    public interface Listener {
        void onRecipeClick(Recipe recipe);
    }

    public static class Row {
        final Recipe recipe;
        final String note;

        public Row(Recipe recipe, String note) {
            this.recipe = recipe;
            this.note = note;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public RecipeAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setRows(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Row row = rows.get(position);
        int count = row.recipe.getIngredients().size();

        holder.tvName.setText(row.recipe.getName());
        holder.tvSubtitle.setText(holder.itemView.getResources()
                .getQuantityString(R.plurals.ingredient_count, count, count)
                + " • " + row.recipe.getDescription());

        if (row.note != null) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText(row.note);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(row.recipe));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvSubtitle;
        final TextView tvNote;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvRecipeName);
            tvSubtitle = v.findViewById(R.id.tvRecipeSubtitle);
            tvNote = v.findViewById(R.id.tvRecipeNote);
        }
    }
}
