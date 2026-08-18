package com.example.tippy;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FinalSummaryAdapter extends RecyclerView.Adapter<FinalSummaryAdapter.ViewHolder> {

    static class FinalRow {
        final Party party;
        final double total;

        FinalRow(Party party, double total) {
            this.party = party;
            this.total = total;
        }
    }

    private final List<FinalRow> rows = new ArrayList<>();
    private String currencySymbol = CurrencyUtils.DEFAULT_SYMBOL;

    void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    void setRows(List<FinalRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_final_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(rows.get(position), currencySymbol);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View finalPartyColorDot;
        private final TextView finalPartyNameText;
        private final TextView finalPartyAmountText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            finalPartyColorDot = itemView.findViewById(R.id.finalPartyColorDot);
            finalPartyNameText = itemView.findViewById(R.id.finalPartyNameText);
            finalPartyAmountText = itemView.findViewById(R.id.finalPartyAmountText);
        }

        void bind(FinalRow row, String currencySymbol) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(ContextCompat.getColor(itemView.getContext(), row.party.getColorResId()));
            finalPartyColorDot.setBackground(dot);

            finalPartyNameText.setText(row.party.getName());
            finalPartyAmountText.setText(CurrencyUtils.format(currencySymbol, row.total));
        }
    }
}
