package com.example.timdahan;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;

import java.util.ArrayList;
import java.util.List;

public class RecordSaleActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private UserSession userSession;
    private int currentUserId;

    private List<Product> productList;
    private Product selectedProduct;
    private AutoCompleteTextView actvProductSearch;
    private TextView tvSelectedProductInfo, tvStockError;

    private Button btnMinus, btnPlus, btnRecordSale;
    private EditText etQuantity;
    private TextView tvSubtotal;

    private RecyclerView rvRecentTransactions;
    private LinearLayout layoutEmptyRecent;
    private TextView btnViewAll;

    private int quantity = 0;
    private double itemPrice = 0.0;
    private boolean isUpdatingQuantityProgrammatically = false;

    private void startBarcodeScanner() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .enableAutoZoom()
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null && !rawValue.trim().isEmpty()) {
                        findAndSelectProductByBarcode(rawValue.trim());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_sale);

        hideNavigationBar();

        databaseHelper = new DatabaseHelper(this);
        userSession = new UserSession(this);
        currentUserId = userSession.getUserId();

        ImageView btnBack = findViewById(R.id.btn_back_sale);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        actvProductSearch = findViewById(R.id.actv_product_search);
        tvSelectedProductInfo = findViewById(R.id.tv_selected_product_info);
        tvStockError = findViewById(R.id.tv_stock_error);

        Button btnScanBarcode = findViewById(R.id.btn_scan_sale_barcode);
        btnMinus = findViewById(R.id.btn_minus);
        btnPlus = findViewById(R.id.btn_plus);
        etQuantity = findViewById(R.id.et_quantity);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        btnRecordSale = findViewById(R.id.btn_record_sale);

        rvRecentTransactions = findViewById(R.id.rv_recent_transactions);
        layoutEmptyRecent = findViewById(R.id.layout_empty_recent_transactions);
        btnViewAll = findViewById(R.id.btn_view_all_transactions);

        if (rvRecentTransactions != null) {
            rvRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
        }

        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(RecordSaleActivity.this, TransactionHistoryActivity.class);
                startActivity(intent);
            });
        }

        loadProductsSearchAdapter();

        // Initially disable controls
        resetSelectionState();

        if (btnScanBarcode != null) {
            btnScanBarcode.setOnClickListener(v -> startBarcodeScanner());
        }

        if (actvProductSearch != null) {
            actvProductSearch.setOnItemClickListener((parent, view, position, id) -> {
                String selectedName = (String) parent.getItemAtPosition(position);
                findAndSelectProductByName(selectedName);
            });

            actvProductSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (selectedProduct != null && !s.toString().trim().equalsIgnoreCase(selectedProduct.getName())) {
                        resetSelectionState();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (etQuantity != null) {
            etQuantity.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isUpdatingQuantityProgrammatically) return;

                    String text = s.toString().trim();
                    if (text.isEmpty()) {
                        quantity = 0;
                    } else {
                        try {
                            quantity = Integer.parseInt(text);
                        } catch (NumberFormatException e) {
                            quantity = 0;
                        }
                    }
                    validateQuantityAndStock();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                setQuantityText(quantity - 1);
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (selectedProduct != null) {
                setQuantityText(quantity + 1);
            }
        });

        btnRecordSale.setOnClickListener(v -> {
            if (selectedProduct == null || quantity <= 0) {
                Toast.makeText(RecordSaleActivity.this, "Please select a valid product and quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            if (quantity > selectedProduct.getStockQuantity()) {
                tvStockError.setText("Only " + selectedProduct.getStockQuantity() + " available");
                tvStockError.setVisibility(View.VISIBLE);
                btnRecordSale.setEnabled(false);
                btnRecordSale.setAlpha(0.5f);
                return;
            }

            showSaleConfirmationDialog();
        });

        loadRecentTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProductsSearchAdapter();
        loadRecentTransactions();
    }

    private void setQuantityText(int q) {
        isUpdatingQuantityProgrammatically = true;
        quantity = q;
        if (etQuantity != null) {
            etQuantity.setText(String.valueOf(q));
            etQuantity.setSelection(etQuantity.getText().length());
        }
        isUpdatingQuantityProgrammatically = false;
        validateQuantityAndStock();
    }

    private void validateQuantityAndStock() {
        if (selectedProduct == null) {
            tvSubtotal.setText("₱0.00");
            if (tvStockError != null) tvStockError.setVisibility(View.GONE);
            btnRecordSale.setEnabled(false);
            btnRecordSale.setAlpha(0.5f);
            return;
        }

        double subtotal = quantity * itemPrice;
        tvSubtotal.setText(String.format("₱%.2f", subtotal));

        if (quantity <= 0) {
            if (tvStockError != null) tvStockError.setVisibility(View.GONE);
            btnRecordSale.setEnabled(false);
            btnRecordSale.setAlpha(0.5f);
        } else if (quantity > selectedProduct.getStockQuantity()) {
            if (tvStockError != null) {
                tvStockError.setText("Only " + selectedProduct.getStockQuantity() + " available");
                tvStockError.setVisibility(View.VISIBLE);
            }
            btnRecordSale.setEnabled(false);
            btnRecordSale.setAlpha(0.5f);
        } else {
            if (tvStockError != null) tvStockError.setVisibility(View.GONE);
            btnRecordSale.setEnabled(true);
            btnRecordSale.setAlpha(1.0f);
        }
    }

    private void showSaleConfirmationDialog() {
        if (selectedProduct == null) return;

        double totalAmount = quantity * itemPrice;
        String confirmMsg = selectedProduct.getName() + "\n\n"
                + quantity + " × ₱" + String.format("%.2f", itemPrice)
                + " = ₱" + String.format("%.2f", totalAmount);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Sale?");
        builder.setMessage(confirmMsg);

        builder.setPositiveButton("Record Sale", (dialog, which) -> {
            boolean success = databaseHelper.recordSale(currentUserId, selectedProduct.getId(), quantity, itemPrice, totalAmount);

            if (success) {
                Toast.makeText(RecordSaleActivity.this, "Sale recorded.", Toast.LENGTH_SHORT).show();

                // Reset search & quantity state
                if (actvProductSearch != null) actvProductSearch.setText("", false);
                resetSelectionState();

                // Refresh adapters & lists
                loadProductsSearchAdapter();
                loadRecentTransactions();
            } else {
                Toast.makeText(RecordSaleActivity.this, "Failed to record sale. Try again.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadProductsSearchAdapter() {
        productList = databaseHelper.getAllProducts(currentUserId);

        List<String> productNames = new ArrayList<>();
        for (Product product : productList) {
            productNames.add(product.getName());
        }

        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_product_dropdown,
                R.id.tv_dropdown_item_name,
                productNames
        );

        if (actvProductSearch != null) {
            actvProductSearch.setAdapter(searchAdapter);
        }
    }

    private void findAndSelectProductByName(String productName) {
        if (productList == null) return;

        for (Product p : productList) {
            if (p.getName().equalsIgnoreCase(productName)) {
                onProductSelected(p);
                return;
            }
        }
    }

    private void onProductSelected(Product product) {
        selectedProduct = product;

        if (actvProductSearch != null) {
            actvProductSearch.setText(product.getName(), false);
        }

        if (tvSelectedProductInfo != null) {
            tvSelectedProductInfo.setVisibility(View.VISIBLE);
            if (product.getStockQuantity() <= 0) {
                tvSelectedProductInfo.setText("OUT OF STOCK | ₱" + String.format("%.2f", product.getPrice()) + " each");
                tvSelectedProductInfo.setTextColor(getResources().getColor(R.color.error_text));
            } else {
                tvSelectedProductInfo.setText("Available Stock: " + product.getStockQuantity() + " left | ₱" + String.format("%.2f", product.getPrice()) + " each");
                tvSelectedProductInfo.setTextColor(getResources().getColor(R.color.button_blue));
            }
        }

        if (product.getStockQuantity() <= 0) {
            if (tvStockError != null) {
                tvStockError.setText("Only 0 available");
                tvStockError.setVisibility(View.VISIBLE);
            }
            if (etQuantity != null) etQuantity.setEnabled(false);
            btnMinus.setEnabled(false);
            btnPlus.setEnabled(false);
            btnMinus.setAlpha(0.5f);
            btnPlus.setAlpha(0.5f);
            setQuantityText(0);
        } else {
            if (etQuantity != null) etQuantity.setEnabled(true);
            itemPrice = product.getPrice();
            btnMinus.setEnabled(true);
            btnPlus.setEnabled(true);
            btnMinus.setAlpha(1.0f);
            btnPlus.setAlpha(1.0f);
            setQuantityText(1);
        }
    }

    private void resetSelectionState() {
        selectedProduct = null;
        itemPrice = 0.0;

        if (tvSelectedProductInfo != null) {
            tvSelectedProductInfo.setVisibility(View.GONE);
        }
        if (tvStockError != null) {
            tvStockError.setVisibility(View.GONE);
        }

        if (etQuantity != null) etQuantity.setEnabled(false);
        if (btnMinus != null) {
            btnMinus.setEnabled(false);
            btnMinus.setAlpha(0.5f);
        }
        if (btnPlus != null) {
            btnPlus.setEnabled(false);
            btnPlus.setAlpha(0.5f);
        }
        if (btnRecordSale != null) {
            btnRecordSale.setEnabled(false);
            btnRecordSale.setAlpha(0.5f);
        }

        setQuantityText(0);
    }

    private void loadRecentTransactions() {
        List<SaleTransaction> allTransactions = databaseHelper.getAllTransactions(currentUserId);

        if (allTransactions.isEmpty()) {
            if (layoutEmptyRecent != null) layoutEmptyRecent.setVisibility(View.VISIBLE);
            if (rvRecentTransactions != null) rvRecentTransactions.setVisibility(View.GONE);
        } else {
            if (layoutEmptyRecent != null) layoutEmptyRecent.setVisibility(View.GONE);
            if (rvRecentTransactions != null) rvRecentTransactions.setVisibility(View.VISIBLE);

            int limit = Math.min(10, allTransactions.size());
            List<SaleTransaction> recentList = allTransactions.subList(0, limit);

            TransactionAdapter adapter = new TransactionAdapter(this, recentList, new TransactionAdapter.OnTransactionClickListener() {
                @Override
                public void onTransactionClick(SaleTransaction transaction) {
                    showTransactionDetailsDialog(transaction);
                }
            });

            if (rvRecentTransactions != null) {
                rvRecentTransactions.setAdapter(adapter);
            }
        }
    }

    private void showTransactionDetailsDialog(SaleTransaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_transaction_details, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvTitle = dialogView.findViewById(R.id.tv_detail_title);
        TextView tvDate = dialogView.findViewById(R.id.tv_detail_date);
        TextView tvTotal = dialogView.findViewById(R.id.tv_detail_total);
        LinearLayout layoutItems = dialogView.findViewById(R.id.layout_detail_items);
        Button btnClose = dialogView.findViewById(R.id.btn_close_detail);

        tvTitle.setText("Transaction #" + transaction.getTransactionNumber());
        tvDate.setText(transaction.getSaleDate());
        tvTotal.setText(String.format("₱%.2f", transaction.getTotalAmount()));

        List<SaleDetailItem> detailItems = databaseHelper.getTransactionDetails(transaction.getSaleId(), currentUserId);

        layoutItems.removeAllViews();
        for (SaleDetailItem item : detailItems) {
            TextView itemView = new TextView(this);
            itemView.setText(item.getQuantity() + "x " + item.getProductName()
                    + " @ ₱" + String.format("%.2f", item.getUnitPrice())
                    + " = ₱" + String.format("%.2f", item.getSubtotal()));
            itemView.setTextSize(14);
            itemView.setPadding(0, 6, 0, 6);
            itemView.setTextColor(getResources().getColor(R.color.text_dark));
            layoutItems.addView(itemView);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void findAndSelectProductByBarcode(String barcode) {
        Product matchedProduct = databaseHelper.getProductByBarcode(barcode, currentUserId);

        if (matchedProduct != null) {
            onProductSelected(matchedProduct);
            Toast.makeText(this, "Found product: " + matchedProduct.getName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No product found with barcode: " + barcode, Toast.LENGTH_LONG).show();
        }
    }

    private void hideNavigationBar() {
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
    }
}