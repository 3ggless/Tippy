package com.example.tippy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.Serializable;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_ITEMS = "extra_items";
    public static final String EXTRA_CURRENCY = "extra_currency";

    private ProgressBar progressBar;
    private MaterialButton btnScan;
    private MaterialButton btnSample;

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Void> takePicturePreview =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), this::processBitmap);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressBar = findViewById(R.id.progressBar);
        btnScan = findViewById(R.id.btnScan);
        btnSample = findViewById(R.id.btnSample);

        btnScan.setOnClickListener(v -> requestCameraPermission.launch(android.Manifest.permission.CAMERA));
        btnSample.setOnClickListener(v -> goToPartySetup(ReceiptParser.sampleResult()));
    }

    private void launchCamera() {
        takePicturePreview.launch(null);
    }

    private void processBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        setLoading(true);
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(this::onTextRecognized)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.no_items_found, Toast.LENGTH_LONG).show();
                });
    }

    private void onTextRecognized(Text visionText) {
        setLoading(false);
        ReceiptParser.ParseResult result = ReceiptParser.parse(visionText.getText());
        if (result.getItems().isEmpty()) {
            Toast.makeText(this, R.string.no_items_found, Toast.LENGTH_LONG).show();
            return;
        }
        goToPartySetup(result);
    }

    private void goToPartySetup(ReceiptParser.ParseResult result) {
        Intent intent = new Intent(this, PartySetupActivity.class);
        intent.putExtra(EXTRA_ITEMS, (Serializable) result.getItems());
        intent.putExtra(EXTRA_CURRENCY, result.getCurrencySymbol());
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnScan.setEnabled(!loading);
        btnSample.setEnabled(!loading);
    }
}
