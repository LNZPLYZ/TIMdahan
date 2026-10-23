package com.example.timdahan;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
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

public class ProductsActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private UserSession userSession;
    private int currentUserId;

    private RecyclerView rvProducts;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutCategoryChips;
    private EditText etSearch;
    private TextView tvTotalCount;

    private String selectedCategory = "All";
    private ProductAdapter adapter;
    private List<Product> productList;

    private interface OnBarcodeScannedListener {
        void onScanned(String barcode);
    }

    private void startBarcodeScanner(OnBarcodeScannedListener listener) {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .enableAutoZoom()
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null && !rawValue.trim().isEmpty()) {
                        listener.onScanned(rawValue.trim());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        hideNavigationBar();

        databaseHelper = new DatabaseHelper(this);
        userSession = new UserSession(this);
        currentUserId = userSession.getUserId();

        rvProducts = findViewById(R.id.rv_products);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutCategoryChips = findViewById(R.id.layout_category_chips);
        etSearch = findViewById(R.id.et_search_products);
        tvTotalCount = findViewById(R.id.tv_products_total_count);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        ImageView btnBack = findViewById(R.id.btn_back_products);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        ImageView btnScanBarcode = findViewById(R.id.btn_scan_products_barcode);
        if (btnScanBarcode != null) {
            btnScanBarcode.setOnClickListener(v -> {
                startBarcodeScanner(scannedBarcode -> {
                    if (etSearch != null) {
                        etSearch.setText(scannedBarcode);
                    }
                });
            });
        }

        Button btnAddProduct = findViewById(R.id.btn_add_product_inventory);
        Button btnEmptyAddProduct = findViewById(R.id.btn_empty_add_product);

        View.OnClickListener openAddDialogListener = v -> showAddEditProductDialog(null);

        if (btnAddProduct != null) {
            btnAddProduct.setOnClickListener(openAddDialogListener);
        }

        if (btnEmptyAddProduct != null) {
            btnEmptyAddProduct.setOnClickListener(openAddDialogListener);
        }

        // Live Search Filter
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterProducts(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Load Products from SQLite for current user
        loadProductsFromDatabase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProductsFromDatabase();
    }

    private void loadProductsFromDatabase() {
        productList = databaseHelper.getAllProducts(currentUserId);

        if (tvTotalCount != null) {
            tvTotalCount.setText("Total Catalog: " + productList.size() + " products");
        }

        loadCategoryChips();

        if (productList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvProducts.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvProducts.setVisibility(View.VISIBLE);

            adapter = new ProductAdapter(this, productList, new ProductAdapter.OnProductClickListener() {
                @Override
                public void onProductClick(Product product) {
                    showAddEditProductDialog(product);
                }

                @Override
                public void onEditClick(Product product) {
                    showAddEditProductDialog(product);
                }

                @Override
                public void onDeleteClick(Product product) {
                    confirmDeleteProduct(product);
                }
            });
            rvProducts.setAdapter(adapter);

            String currentQuery = (etSearch != null) ? etSearch.getText().toString() : "";
            filterProducts(currentQuery);
        }
    }

    private void loadCategoryChips() {
        if (layoutCategoryChips == null) return;
        layoutCategoryChips.removeAllViews();

        List<DatabaseHelper.CategoryCount> categories = databaseHelper.getCategoryCounts(currentUserId);

        for (DatabaseHelper.CategoryCount cat : categories) {
            TextView chip = new TextView(this);
            chip.setText(cat.categoryName + " (" + cat.count + ")");
            chip.setTextSize(13);
            chip.setTypeface(null, Typeface.BOLD);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            chip.setPadding(32, 16, 32, 16);

            if (cat.categoryName.equalsIgnoreCase(selectedCategory)) {
                chip.setBackgroundResource(R.drawable.rounded_chip_active);
                chip.setTextColor(getResources().getColor(R.color.white));
            } else {
                chip.setBackgroundResource(R.drawable.rounded_chip_inactive);
                chip.setTextColor(getResources().getColor(R.color.text_dark));
            }

            chip.setOnClickListener(v -> {
                selectedCategory = cat.categoryName;
                loadCategoryChips();
                String currentQuery = (etSearch != null) ? etSearch.getText().toString() : "";
                filterProducts(currentQuery);
            });

            layoutCategoryChips.addView(chip);
        }
    }

    private void filterProducts(String query) {
        if (productList == null) return;

        List<Product> filteredList = new ArrayList<>();
        String lowerQuery = query.trim().toLowerCase();

        for (Product product : productList) {
            String pCat = (product.getCategory() == null || product.getCategory().trim().isEmpty()) ? "Others" : product.getCategory().trim();
            boolean matchesCategory = selectedCategory.equalsIgnoreCase("All") || pCat.equalsIgnoreCase(selectedCategory);

            boolean matchesQuery = lowerQuery.isEmpty() ||
                    product.getName().toLowerCase().contains(lowerQuery) ||
                    pCat.toLowerCase().contains(lowerQuery) ||
                    (product.getBarcode() != null && product.getBarcode().contains(lowerQuery));

            if (matchesCategory && matchesQuery) {
                filteredList.add(product);
            }
        }

        if (adapter != null) {
            adapter.setFilteredList(filteredList);
        }
    }

    private void confirmDeleteProduct(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Product");
        builder.setMessage("Are you sure you want to delete '" + product.getName() + "'?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            databaseHelper.deleteProduct(product.getId(), currentUserId);
            Toast.makeText(ProductsActivity.this, "Product Deleted", Toast.LENGTH_SHORT).show();
            loadProductsFromDatabase();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddEditProductDialog(Product existingProduct) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_product, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etName = dialogView.findViewById(R.id.et_product_name);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_product_category);
        EditText etPrice = dialogView.findViewById(R.id.et_product_price);
        EditText etStock = dialogView.findViewById(R.id.et_product_stock);
        EditText etMinStock = dialogView.findViewById(R.id.et_product_min_stock);
        EditText etBarcode = dialogView.findViewById(R.id.et_product_barcode);
        Button btnScanBarcode = dialogView.findViewById(R.id.btn_scan_barcode_dialog);

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_product);
        Button btnSave = dialogView.findViewById(R.id.btn_save_product);

        if (btnScanBarcode != null) {
            btnScanBarcode.setOnClickListener(v -> {
                startBarcodeScanner(scannedBarcode -> {
                    if (etBarcode != null) {
                        etBarcode.setText(scannedBarcode);
                        Toast.makeText(ProductsActivity.this, "Barcode Scanned: " + scannedBarcode, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        String[] categories = {
                "Beverages",
                "Canned",
                "Snacks",
                "Candies",
                "Noodles",
                "Groceries",
                "Dairy",
                "Bakery",
                "Frozen",
                "Condiments",
                "Personal",
                "Laundry",
                "Cleaning",
                "Household",
                "Baby",
                "Health",
                "School",
                "Electronics",
                "Hardware",
                "Tobacco",
                "Alcohol",
                "Produce",
                "Meat",
                "Seafood",
                "Pet",
                "Automotive",
                "Accessories",
                "Services",
                "Others"
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Pre-fill fields if editing existing product
        boolean isEditMode = (existingProduct != null);
        if (isEditMode) {
            tvTitle.setText("Edit Product");
            btnSave.setText("Save");

            etName.setText(existingProduct.getName());
            etPrice.setText(String.valueOf(existingProduct.getPrice()));
            etStock.setText(String.valueOf(existingProduct.getStockQuantity()));
            etMinStock.setText(String.valueOf(existingProduct.getMinimumStock()));
            etBarcode.setText(existingProduct.getBarcode());

            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(existingProduct.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        } else {
            tvTitle.setText("Add Product");
            btnSave.setText("Add Product");
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String priceStr = etPrice.getText().toString().trim();
            String stockStr = etStock.getText().toString().trim();
            String minStockStr = etMinStock.getText().toString().trim();
            String barcode = etBarcode.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Product name is required.");
                etName.requestFocus();
                return;
            }

            if (priceStr.isEmpty()) {
                etPrice.setError("Enter a valid selling price.");
                etPrice.requestFocus();
                return;
            }

            if (stockStr.isEmpty()) {
                etStock.setError("Enter a valid stock quantity.");
                etStock.requestFocus();
                return;
            }

            double price = Double.parseDouble(priceStr);
            int stock = Integer.parseInt(stockStr);
            int minStock = minStockStr.isEmpty() ? 5 : Integer.parseInt(minStockStr);

            if (isEditMode) {
                existingProduct.setName(name);
                existingProduct.setCategory(category);
                existingProduct.setPrice(price);
                existingProduct.setStockQuantity(stock);
                existingProduct.setMinimumStock(minStock);
                existingProduct.setBarcode(barcode);

                databaseHelper.updateProduct(existingProduct, currentUserId);
                Toast.makeText(ProductsActivity.this, "Product Updated Successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Product newProduct = new Product(barcode, name, category, price, stock, minStock);
                databaseHelper.addProduct(newProduct, currentUserId);
                Toast.makeText(ProductsActivity.this, "Product Saved Successfully!", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
            loadProductsFromDatabase();
        });

        dialog.show();
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