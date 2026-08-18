package com.example.tippy;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PartySetupActivity extends AppCompatActivity {

    private static final int MIN_PARTIES = 1;
    private static final int MAX_PARTIES = 4;

    private int partyCount = 2;
    private ArrayList<ReceiptItem> items;
    private String currencySymbol;

    private TextView partyCountText;
    private LinearLayout partyNamesContainer;
    private final List<TextInputEditText> nameInputs = new ArrayList<>();
    private final List<String> savedNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_setup);

        View root = findViewById(R.id.partySetupRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        @SuppressWarnings("unchecked")
        ArrayList<ReceiptItem> loadedItems =
                (ArrayList<ReceiptItem>) getIntent().getSerializableExtra(MainActivity.EXTRA_ITEMS);
        items = loadedItems;
        currencySymbol = getIntent().getStringExtra(MainActivity.EXTRA_CURRENCY);
        if (currencySymbol == null) {
            currencySymbol = CurrencyUtils.NO_SYMBOL;
        }

        if (items == null || items.isEmpty()) {
            finish();
            return;
        }

        partyCountText = findViewById(R.id.partyCountText);
        partyNamesContainer = findViewById(R.id.partyNamesContainer);
        MaterialButton btnDecrease = findViewById(R.id.btnDecreaseParties);
        MaterialButton btnIncrease = findViewById(R.id.btnIncreaseParties);
        MaterialButton btnContinue = findViewById(R.id.btnContinue);

        btnDecrease.setOnClickListener(v -> updatePartyCount(partyCount - 1));
        btnIncrease.setOnClickListener(v -> updatePartyCount(partyCount + 1));
        btnContinue.setOnClickListener(v -> openSplitScreen());

        updatePartyCount(partyCount);
    }

    private void updatePartyCount(int newCount) {
        partyCount = Math.max(MIN_PARTIES, Math.min(MAX_PARTIES, newCount));
        partyCountText.setText(String.valueOf(partyCount));
        rebuildNameInputs();
    }

    private void rebuildNameInputs() {
        saveCurrentNames();
        partyNamesContainer.removeAllViews();
        nameInputs.clear();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < partyCount; i++) {
            View row = inflater.inflate(R.layout.item_party_name_input, partyNamesContainer, false);
            View colorIndicator = row.findViewById(R.id.partyColorIndicator);
            TextInputEditText nameInput = row.findViewById(R.id.partyNameInput);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(ContextCompat.getColor(this, Party.PARTY_COLORS[i]));
            drawable.setCornerRadius(8f);
            colorIndicator.setBackground(drawable);

            nameInput.setHint(getString(R.string.party_name_hint) + " (" + Party.DEFAULT_NAMES[i] + ")");
            if (i < savedNames.size() && !savedNames.get(i).isEmpty()) {
                nameInput.setText(savedNames.get(i));
            }

            nameInputs.add(nameInput);
            partyNamesContainer.addView(row);
        }
    }

    private void saveCurrentNames() {
        savedNames.clear();
        for (TextInputEditText input : nameInputs) {
            savedNames.add(input.getText() != null ? input.getText().toString().trim() : "");
        }
    }

    private void openSplitScreen() {
        saveCurrentNames();
        ArrayList<Party> parties = new ArrayList<>();
        for (int i = 0; i < partyCount; i++) {
            String name = i < savedNames.size() && !savedNames.get(i).isEmpty()
                    ? savedNames.get(i)
                    : Party.DEFAULT_NAMES[i];
            parties.add(new Party(i, name));
        }

        Intent intent = new Intent(this, SplitActivity.class);
        intent.putExtra(SplitActivity.EXTRA_ITEMS, (Serializable) items);
        intent.putExtra(SplitActivity.EXTRA_PARTIES, (Serializable) parties);
        intent.putExtra(SplitActivity.EXTRA_CURRENCY, currencySymbol);
        intent.putExtra(SplitActivity.EXTRA_SINGLE_PARTY, partyCount == 1);
        startActivity(intent);
    }
}
