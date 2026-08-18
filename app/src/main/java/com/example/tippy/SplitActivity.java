package com.example.tippy;

import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SplitActivity extends AppCompatActivity {

    public static final String EXTRA_ITEMS = "split_items";
    public static final String EXTRA_PARTIES = "split_parties";
    public static final String EXTRA_CURRENCY = "split_currency";
    public static final String EXTRA_SINGLE_PARTY = "split_single_party";

    private static final float SWIPE_THRESHOLD = 120f;

    private ArrayList<ReceiptItem> items;
    private ArrayList<Party> parties;
    private String currencySymbol;
    private boolean singlePartyMode;
    private int tipPercent = 0;
    private boolean updatingTipFromCode;

    private TextView tipLabel;
    private TextView itemProgressText;
    private TextView itemNameText;
    private TextView itemPriceText;
    private MaterialCardView itemCard;
    private TextView labelLeft;
    private TextView labelRight;
    private TextView labelUp;
    private TextView labelDown;
    private LinearLayout swipeSection;
    private LinearLayout completionSection;
    private TextView completionTitle;
    private TextView receiptSubtotalText;
    private TextView receiptTotalWithTipText;
    private View grandTotalBar;

    private Slider tipSlider;
    private TextInputEditText customTipInput;

    private PartyTotalAdapter partyTotalAdapter;
    private FinalSummaryAdapter finalSummaryAdapter;
    private int currentItemIndex = 0;
    private boolean splitComplete;

    private float downX;
    private float downY;
    private float cardStartX;
    private float cardStartY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split);

        View root = findViewById(R.id.splitRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        @SuppressWarnings("unchecked")
        ArrayList<ReceiptItem> loadedItems =
                (ArrayList<ReceiptItem>) getIntent().getSerializableExtra(EXTRA_ITEMS);
        @SuppressWarnings("unchecked")
        ArrayList<Party> loadedParties =
                (ArrayList<Party>) getIntent().getSerializableExtra(EXTRA_PARTIES);
        items = loadedItems;
        parties = loadedParties;
        currencySymbol = getIntent().getStringExtra(EXTRA_CURRENCY);
        singlePartyMode = getIntent().getBooleanExtra(EXTRA_SINGLE_PARTY, false);

        if (currencySymbol == null) {
            currencySymbol = CurrencyUtils.NO_SYMBOL;
        }
        if (items == null || parties == null || parties.isEmpty()) {
            finish();
            return;
        }

        bindViews();
        setupTipControls();
        setupSwipeGestures();
        setupButtons();
        updateDirectionLabels();

        if (singlePartyMode) {
            assignAllItemsToParty(0);
            showCompletionUi();
        } else {
            grandTotalBar.setVisibility(View.GONE);
            showCurrentItem();
        }
        updateTotals();
    }

    private void bindViews() {
        tipLabel = findViewById(R.id.tipLabel);
        itemProgressText = findViewById(R.id.itemProgressText);
        itemNameText = findViewById(R.id.itemNameText);
        itemPriceText = findViewById(R.id.itemPriceText);
        itemCard = findViewById(R.id.itemCard);
        labelLeft = findViewById(R.id.labelLeft);
        labelRight = findViewById(R.id.labelRight);
        labelUp = findViewById(R.id.labelUp);
        labelDown = findViewById(R.id.labelDown);
        swipeSection = findViewById(R.id.swipeSection);
        completionSection = findViewById(R.id.completionSection);
        completionTitle = findViewById(R.id.completionTitle);
        receiptSubtotalText = findViewById(R.id.receiptSubtotalText);
        receiptTotalWithTipText = findViewById(R.id.receiptTotalWithTipText);
        grandTotalBar = findViewById(R.id.grandTotalBar);
        tipSlider = findViewById(R.id.tipSlider);
        customTipInput = findViewById(R.id.customTipInput);

        RecyclerView partyTotalsRecycler = findViewById(R.id.partyTotalsRecycler);
        partyTotalAdapter = new PartyTotalAdapter();
        partyTotalAdapter.setCurrencySymbol(currencySymbol);
        partyTotalsRecycler.setLayoutManager(new LinearLayoutManager(this));
        partyTotalsRecycler.setAdapter(partyTotalAdapter);

        RecyclerView finalSummaryRecycler = findViewById(R.id.finalSummaryRecycler);
        finalSummaryAdapter = new FinalSummaryAdapter();
        finalSummaryAdapter.setCurrencySymbol(currencySymbol);
        finalSummaryRecycler.setLayoutManager(new LinearLayoutManager(this));
        finalSummaryRecycler.setAdapter(finalSummaryAdapter);
    }

    private void setupTipControls() {
        tipSlider.setValue(tipPercent);
        updateTipLabel(tipPercent);

        tipSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !updatingTipFromCode) {
                setTipPercent(Math.round(value), true);
            }
        });

        customTipInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyCustomTipInput();
                customTipInput.clearFocus();
                return true;
            }
            return false;
        });

        customTipInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updatingTipFromCode || s == null || s.toString().trim().isEmpty()) {
                    return;
                }
                applyCustomTipInput();
            }
        });
    }

    private void applyCustomTipInput() {
        if (customTipInput.getText() == null) {
            return;
        }
        String text = customTipInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            float value = Float.parseFloat(text);
            int rounded = Math.round(Math.max(0f, Math.min(value, 100f)));
            setTipPercent(rounded, false);
        } catch (NumberFormatException ignored) {
        }
    }

    private void setTipPercent(int percent, boolean syncSlider) {
        tipPercent = percent;
        updatingTipFromCode = true;
        updateTipLabel(tipPercent);
        if (syncSlider && percent <= tipSlider.getValueTo()) {
            tipSlider.setValue(percent);
        }
        if (percent == 0) {
            customTipInput.setText("");
        } else {
            String customText = String.valueOf(percent);
            if (!customText.equals(customTipInput.getText() != null ? customTipInput.getText().toString() : "")) {
                customTipInput.setText(customText);
                customTipInput.setSelection(customText.length());
            }
        }
        updatingTipFromCode = false;
        updateTotals();
    }

    private void updateTipLabel(int percent) {
        tipLabel.setText(getString(R.string.tip_percent, String.valueOf(percent)));
    }

    private void setupButtons() {
        MaterialButton btnEditParties = findViewById(R.id.btnEditParties);
        MaterialButton btnStartOver = findViewById(R.id.btnStartOver);

        btnEditParties.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnStartOver.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void updateDirectionLabels() {
        labelLeft.setVisibility(parties.size() > 0 ? View.VISIBLE : View.GONE);
        labelRight.setVisibility(parties.size() > 1 ? View.VISIBLE : View.GONE);
        labelUp.setVisibility(parties.size() > 2 ? View.VISIBLE : View.GONE);
        labelDown.setVisibility(parties.size() > 3 ? View.VISIBLE : View.GONE);

        if (parties.size() > 0) {
            labelLeft.setText("← " + parties.get(0).getName());
            labelLeft.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.party_red));
        }
        if (parties.size() > 1) {
            labelRight.setText(parties.get(1).getName() + " →");
            labelRight.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.party_blue));
        }
        if (parties.size() > 2) {
            labelUp.setText("↑ " + parties.get(2).getName());
            labelUp.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.party_green));
        }
        if (parties.size() > 3) {
            labelDown.setText("↓ " + parties.get(3).getName());
            labelDown.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.party_yellow));
        }
    }

    private void setupSwipeGestures() {
        itemCard.setOnTouchListener((v, event) -> {
            if (splitComplete || !itemCard.isShown() || currentItemIndex >= items.size()) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    cardStartX = itemCard.getTranslationX();
                    cardStartY = itemCard.getTranslationY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - downX;
                    float deltaY = event.getRawY() - downY;
                    itemCard.setTranslationX(cardStartX + deltaX);
                    itemCard.setTranslationY(cardStartY + deltaY);
                    itemCard.setRotation(deltaX / 25f);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float totalDeltaX = event.getRawX() - downX;
                    float totalDeltaY = event.getRawY() - downY;
                    int partyIndex = resolvePartyFromSwipe(totalDeltaX, totalDeltaY);
                    if (partyIndex >= 0) {
                        animateAssignToParty(partyIndex, totalDeltaX, totalDeltaY);
                    } else {
                        resetCardPosition();
                    }
                    return true;

                default:
                    return false;
            }
        });
    }

    private int resolvePartyFromSwipe(float deltaX, float deltaY) {
        if (Math.abs(deltaX) < SWIPE_THRESHOLD && Math.abs(deltaY) < SWIPE_THRESHOLD) {
            return -1;
        }

        if (Math.abs(deltaX) >= Math.abs(deltaY)) {
            if (deltaX <= -SWIPE_THRESHOLD && parties.size() > 0) {
                return 0;
            }
            if (deltaX >= SWIPE_THRESHOLD && parties.size() > 1) {
                return 1;
            }
        } else {
            if (deltaY <= -SWIPE_THRESHOLD && parties.size() > 2) {
                return 2;
            }
            if (deltaY >= SWIPE_THRESHOLD && parties.size() > 3) {
                return 3;
            }
        }
        return -1;
    }

    private void animateAssignToParty(int partyIndex, float deltaX, float deltaY) {
        itemCard.animate()
                .translationX(deltaX * 3f)
                .translationY(deltaY * 3f)
                .alpha(0f)
                .rotation(deltaX / 10f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        assignCurrentItemToParty(partyIndex);
                    }
                })
                .start();
    }

    private void assignCurrentItemToParty(int partyIndex) {
        if (currentItemIndex >= items.size()) {
            return;
        }

        items.get(currentItemIndex).setAssignedPartyIndex(partyIndex);
        currentItemIndex++;

        itemCard.setAlpha(1f);
        itemCard.setTranslationX(0f);
        itemCard.setTranslationY(0f);
        itemCard.setRotation(0f);

        if (currentItemIndex >= items.size()) {
            showCompletionUi();
        } else {
            showCurrentItem();
        }
        updateTotals();
    }

    private void assignAllItemsToParty(int partyIndex) {
        for (ReceiptItem item : items) {
            item.setAssignedPartyIndex(partyIndex);
        }
        currentItemIndex = items.size();
    }

    private void resetCardPosition() {
        itemCard.animate()
                .translationX(0f)
                .translationY(0f)
                .rotation(0f)
                .setDuration(150)
                .start();
    }

    private void showCurrentItem() {
        if (currentItemIndex >= items.size()) {
            showCompletionUi();
            return;
        }

        itemCard.setVisibility(View.VISIBLE);
        ReceiptItem item = items.get(currentItemIndex);
        itemNameText.setText(item.getName());
        itemPriceText.setText(CurrencyUtils.format(currencySymbol, item.getPrice()));
        itemProgressText.setText(getString(R.string.item_progress, currentItemIndex + 1, items.size()));
    }

    private void showCompletionUi() {
        splitComplete = true;
        swipeSection.setVisibility(View.GONE);
        findViewById(R.id.partyTotalsRecycler).setVisibility(View.GONE);
        completionSection.setVisibility(View.VISIBLE);
        grandTotalBar.setVisibility(View.VISIBLE);

        if (singlePartyMode && parties.size() == 1) {
            completionTitle.setText(parties.get(0).getName());
        } else {
            completionTitle.setText(R.string.split_complete);
        }
    }

    private void updateTotals() {
        List<PartyTotalAdapter.PartySummary> compactSummaries = new ArrayList<>();
        List<FinalSummaryAdapter.FinalRow> finalRows = new ArrayList<>();

        double receiptSubtotal = 0;
        for (ReceiptItem item : items) {
            receiptSubtotal += item.getPrice();
        }
        double receiptTotalWithTip = receiptSubtotal * (1 + tipPercent / 100.0);

        for (Party party : parties) {
            double subtotal = 0;
            List<String> itemNames = new ArrayList<>();
            List<String> itemLines = new ArrayList<>();

            for (ReceiptItem item : items) {
                if (item.getAssignedPartyIndex() == party.getIndex()) {
                    subtotal += item.getPrice();
                    itemNames.add(item.getName());
                    itemLines.add(item.getName() + "  " + CurrencyUtils.format(currencySymbol, item.getPrice()));
                }
            }

            double tip = subtotal * tipPercent / 100.0;
            double total = subtotal + tip;

            compactSummaries.add(new PartyTotalAdapter.PartySummary(party, total, itemNames));
            finalRows.add(new FinalSummaryAdapter.FinalRow(party, total, itemLines));
        }

        if (!splitComplete) {
            partyTotalAdapter.setSummaries(compactSummaries);
        }
        finalSummaryAdapter.setRows(finalRows);

        receiptSubtotalText.setText(getString(
                R.string.receipt_subtotal,
                CurrencyUtils.format(currencySymbol, receiptSubtotal)
        ));
        receiptTotalWithTipText.setText(getString(
                R.string.receipt_total_with_tip,
                CurrencyUtils.format(currencySymbol, receiptTotalWithTip)
        ));
    }
}
