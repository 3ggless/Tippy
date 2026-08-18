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

public class PartyTotalAdapter extends RecyclerView.Adapter<PartyTotalAdapter.ViewHolder> {

    static class PartySummary {
        final Party party;
        final double total;
        final List<String> itemNames;

        PartySummary(Party party, double total, List<String> itemNames) {
            this.party = party;
            this.total = total;
            this.itemNames = itemNames;
        }
    }

    private final List<PartySummary> summaries = new ArrayList<>();
    private String currencySymbol = CurrencyUtils.DEFAULT_SYMBOL;

    void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    void setSummaries(List<PartySummary> newSummaries) {
        summaries.clear();
        summaries.addAll(newSummaries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_party_total, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(summaries.get(position), currencySymbol);
    }

    @Override
    public int getItemCount() {
        return summaries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View partyColorDot;
        private final TextView partyNameText;
        private final TextView partyTotalText;
        private final TextView partyItemsText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            partyColorDot = itemView.findViewById(R.id.partyColorDot);
            partyNameText = itemView.findViewById(R.id.partyNameText);
            partyTotalText = itemView.findViewById(R.id.partyTotalText);
            partyItemsText = itemView.findViewById(R.id.partyItemsText);
        }

        void bind(PartySummary summary, String currencySymbol) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(ContextCompat.getColor(itemView.getContext(), summary.party.getColorResId()));
            partyColorDot.setBackground(dot);

            partyNameText.setText(summary.party.getName());
            partyTotalText.setText(CurrencyUtils.format(currencySymbol, summary.total));

            if (summary.itemNames.isEmpty()) {
                partyItemsText.setText(R.string.no_items_yet);
            } else {
                partyItemsText.setText(String.join(", ", summary.itemNames));
            }
        }
    }
}
