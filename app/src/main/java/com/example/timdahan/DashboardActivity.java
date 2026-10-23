package com.example.timdahan;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private UserSession userSession;
    private int currentUserId;

    private TextView  tvSalesAmount, tvTransactionsCount, tvProductsCount, tvLowStockCount;
    private LinearLayout layoutEmptyState, layoutActiveState;
    private RecyclerView rvLowStock;

    // Export Backup File Launcher
    private final ActivityResultLauncher<Intent> exportBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        saveBackupToFile(uri);
                    }
                }
            }
    );

    // Import/Restore Backup File Launcher
    private final ActivityResultLauncher<Intent> importBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        readBackupFromFile(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userSession = new UserSession(this);

        // Check if user is logged in
        if (!userSession.isLoggedIn()) {
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        hideNavigationBar();

        databaseHelper = new DatabaseHelper(this);
        currentUserId = userSession.getUserId();

        tvSalesAmount = findViewById(R.id.tv_dash_sales_amount);
        tvTransactionsCount = findViewById(R.id.tv_dash_transactions_count);
        tvProductsCount = findViewById(R.id.tv_dash_products_count);
        tvLowStockCount = findViewById(R.id.tv_dash_low_stock_count);

        layoutEmptyState = findViewById(R.id.layout_dashboard_empty_state);
        layoutActiveState = findViewById(R.id.layout_dashboard_active_state);

        rvLowStock = findViewById(R.id.rv_low_stock);
        if (rvLowStock != null) {
            rvLowStock.setLayoutManager(new LinearLayoutManager(this));
        }

        Button btnAddProduct = findViewById(R.id.btn_add_product_dashboard);
        if (btnAddProduct != null) {
            btnAddProduct.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ProductsActivity.class)));
        }

        ImageView btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showSettingsDialog());
        }

        LinearLayout btnOpenTransactions = findViewById(R.id.btn_open_transactions_dashboard);
        if (btnOpenTransactions != null) {
            btnOpenTransactions.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, TransactionHistoryActivity.class)));
        }

        LinearLayout navProducts = findViewById(R.id.nav_products);
        LinearLayout navSales = findViewById(R.id.nav_sales);
        LinearLayout navReports = findViewById(R.id.nav_reports);

        if (navProducts != null) {
            navProducts.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ProductsActivity.class)));
        }

        if (navSales != null) {
            navSales.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, RecordSaleActivity.class)));
        }

        if (navReports != null) {
            navReports.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ReportsActivity.class)));
        }

        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userSession.isLoggedIn()) {
            loadDashboardData();
        }
    }

    private void loadDashboardData() {

        int productsCount = databaseHelper.getProductsCount(currentUserId);
        List<Product> lowStockList = databaseHelper.getLowStockProducts(currentUserId);

        double todaysSales = databaseHelper.getTodaysSalesTotal(currentUserId);
        int todaysTransactions = databaseHelper.getTodaysSalesCount(currentUserId);

        if (tvSalesAmount != null) {
            tvSalesAmount.setText(String.format("₱%.2f", todaysSales));
        }

        if (tvTransactionsCount != null) {
            tvTransactionsCount.setText(todaysTransactions + "");
        }

        if (tvProductsCount != null) {
            tvProductsCount.setText(String.valueOf(productsCount));
        }

        if (tvLowStockCount != null) {
            tvLowStockCount.setText(lowStockList.size() + " Items");
        }

        if (productsCount == 0) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            if (layoutActiveState != null) layoutActiveState.setVisibility(View.GONE);
        } else {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            if (layoutActiveState != null) layoutActiveState.setVisibility(View.VISIBLE);

            if (rvLowStock != null && !lowStockList.isEmpty()) {
                ProductAdapter lowStockAdapter = new ProductAdapter(this, lowStockList, new ProductAdapter.OnProductClickListener() {
                    @Override
                    public void onProductClick(Product product) {
                        startActivity(new Intent(DashboardActivity.this, ProductsActivity.class));
                    }
                });
                rvLowStock.setAdapter(lowStockAdapter);
            }
        }
    }

    private void showSettingsDialog() {
        String[] options = {
                "☁️ Backup to Cloud",
                "☁️ Restore from Cloud",
                "📁 Local File Backup",
                "📁 Local File Restore",
                "🚪 Log Out"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Account & Cloud Backup Settings");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // Backup to Cloud (Firebase)
                    Toast.makeText(DashboardActivity.this, "Uploading backup to Cloud...", Toast.LENGTH_SHORT).show();
                    BackupManager.uploadToCloud(DashboardActivity.this, currentUserId, new BackupManager.CloudCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(DashboardActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                } else if (which == 1) {
                    // Restore from Cloud (Firebase)
                    Toast.makeText(DashboardActivity.this, "Downloading backup from Cloud...", Toast.LENGTH_SHORT).show();
                    BackupManager.downloadFromCloud(DashboardActivity.this, currentUserId, new BackupManager.CloudCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_LONG).show();
                            loadDashboardData();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(DashboardActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                } else if (which == 2) {
                    // Local File Backup
                    triggerExportBackup();
                } else if (which == 3) {
                    // Local File Restore
                    triggerImportBackup();
                } else if (which == 4) {
                    // Log Out
                    userSession.logoutUser();
                    Toast.makeText(DashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void triggerExportBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "TIMdahan_Backup_" + System.currentTimeMillis() + ".json");
        exportBackupLauncher.launch(intent);
    }

    private void triggerImportBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        importBackupLauncher.launch(intent);
    }

    private void saveBackupToFile(Uri uri) {
        try {
            String jsonContent = BackupManager.exportDataToJson(this, currentUserId);
            if (jsonContent == null) {
                Toast.makeText(this, "Failed to generate backup data", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(jsonContent.getBytes());
                outputStream.close();
                Toast.makeText(this, "Backup Saved Successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving backup file", Toast.LENGTH_SHORT).show();
        }
    }

    private void readBackupFromFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                inputStream.close();

                String jsonContent = result.toString("UTF-8");
                BackupManager.RestoreResult restoreResult = BackupManager.restoreDataFromJsonDetailed(this, currentUserId, jsonContent);

                if (restoreResult.success) {
                    Toast.makeText(this, "Backup Restored Successfully!", Toast.LENGTH_LONG).show();
                    loadDashboardData();
                } else {
                    Toast.makeText(this, restoreResult.errorMessage != null ? restoreResult.errorMessage : "Failed to restore backup file", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading backup file", Toast.LENGTH_SHORT).show();
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