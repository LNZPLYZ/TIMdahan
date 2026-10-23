package com.example.timdahan;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private UserSession userSession;
    private int currentUserId;

    private TextView chipToday, chip7Days, chip30Days, chipCustom;
    private LinearLayout layoutCustomDatePickers;
    private Button btnStartDate, btnEndDate;

    private TextView tvTotalSales, tvTotalTransactions, tvUnitsSold, tvAvgOrderValue;
    private SalesChartView chartSalesTrend;
    private LinearLayout layoutEmptyState, layoutActiveState;
    private RecyclerView rvBestSellers;

    private String selectedPeriodFilter = "7DAYS"; // Default filter: Last 7 Days
    private String customStartDate = "";
    private String customEndDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        hideNavigationBar();

        databaseHelper = new DatabaseHelper(this);
        userSession = new UserSession(this);
        currentUserId = userSession.getUserId();

        ImageView btnBack = findViewById(R.id.btn_back_reports);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Filter Chips
        chipToday = findViewById(R.id.chip_today);
        chip7Days = findViewById(R.id.chip_7days);
        chip30Days = findViewById(R.id.chip_30days);
        chipCustom = findViewById(R.id.chip_custom);

        // Custom Date Picker Layout
        layoutCustomDatePickers = findViewById(R.id.layout_custom_date_pickers);
        btnStartDate = findViewById(R.id.btn_start_date);
        btnEndDate = findViewById(R.id.btn_end_date);

        // Metric Cards
        tvTotalSales = findViewById(R.id.tv_reports_total_sales);
        tvTotalTransactions = findViewById(R.id.tv_reports_total_transactions);
        tvUnitsSold = findViewById(R.id.tv_reports_units_sold);
        tvAvgOrderValue = findViewById(R.id.tv_reports_avg_order_value);

        // Chart & Best Sellers
        chartSalesTrend = findViewById(R.id.chart_sales_trend);
        layoutEmptyState = findViewById(R.id.layout_reports_empty_state);
        layoutActiveState = findViewById(R.id.layout_reports_active_state);

        rvBestSellers = findViewById(R.id.rv_best_sellers);
        if (rvBestSellers != null) {
            rvBestSellers.setLayoutManager(new LinearLayoutManager(this));
        }

        // Initialize Custom Dates to today
        customStartDate = getTodayDateString();
        customEndDate = getTodayDateString();
        btnStartDate.setText("Start: " + customStartDate);
        btnEndDate.setText("End: " + customEndDate);

        setupFilterChipListeners();
        setupDatePickerListeners();

        Button btnRecordSale = findViewById(R.id.btn_record_sale_reports);
        if (btnRecordSale != null) {
            btnRecordSale.setOnClickListener(v -> startActivity(new Intent(ReportsActivity.this, RecordSaleActivity.class)));
        }

        updateChipUIState();
        loadReportsData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReportsData();
    }

    private void setupFilterChipListeners() {
        chipToday.setOnClickListener(v -> {
            selectedPeriodFilter = "TODAY";
            updateChipUIState();
            loadReportsData();
        });

        chip7Days.setOnClickListener(v -> {
            selectedPeriodFilter = "7DAYS";
            updateChipUIState();
            loadReportsData();
        });

        chip30Days.setOnClickListener(v -> {
            selectedPeriodFilter = "30DAYS";
            updateChipUIState();
            loadReportsData();
        });

        chipCustom.setOnClickListener(v -> {
            selectedPeriodFilter = "CUSTOM";
            updateChipUIState();
            loadReportsData();
        });
    }

    private void setupDatePickerListeners() {
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            String selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);

            if (isStartDate) {
                if (!customEndDate.isEmpty() && selectedDate.compareTo(customEndDate) > 0) {
                    Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show();
                    return;
                }
                customStartDate = selectedDate;
                btnStartDate.setText("Start: " + customStartDate);
            } else {
                if (!customStartDate.isEmpty() && selectedDate.compareTo(customStartDate) < 0) {
                    Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show();
                    return;
                }
                customEndDate = selectedDate;
                btnEndDate.setText("End: " + customEndDate);
            }

            loadReportsData();
        }, year, month, day);

        dialog.show();
    }

    private void updateChipUIState() {
        chipToday.setBackgroundResource("TODAY".equals(selectedPeriodFilter) ? R.drawable.rounded_chip_active : R.drawable.rounded_chip_inactive);
        chipToday.setTextColor("TODAY".equals(selectedPeriodFilter) ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_dark));

        chip7Days.setBackgroundResource("7DAYS".equals(selectedPeriodFilter) ? R.drawable.rounded_chip_active : R.drawable.rounded_chip_inactive);
        chip7Days.setTextColor("7DAYS".equals(selectedPeriodFilter) ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_dark));

        chip30Days.setBackgroundResource("30DAYS".equals(selectedPeriodFilter) ? R.drawable.rounded_chip_active : R.drawable.rounded_chip_inactive);
        chip30Days.setTextColor("30DAYS".equals(selectedPeriodFilter) ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_dark));

        chipCustom.setBackgroundResource("CUSTOM".equals(selectedPeriodFilter) ? R.drawable.rounded_chip_active : R.drawable.rounded_chip_inactive);
        chipCustom.setTextColor("CUSTOM".equals(selectedPeriodFilter) ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_dark));

        if (layoutCustomDatePickers != null) {
            layoutCustomDatePickers.setVisibility("CUSTOM".equals(selectedPeriodFilter) ? View.VISIBLE : View.GONE);
        }
    }

    private void loadReportsData() {
        String startDate;
        String endDate = getTodayDateString();

        switch (selectedPeriodFilter) {
            case "TODAY":
                startDate = getTodayDateString();
                break;
            case "30DAYS":
                startDate = getPastDateString(29);
                break;
            case "CUSTOM":
                startDate = customStartDate.isEmpty() ? getTodayDateString() : customStartDate;
                endDate = customEndDate.isEmpty() ? getTodayDateString() : customEndDate;
                break;
            case "7DAYS":
            default:
                startDate = getPastDateString(6);
                break;
        }

        double totalSales = databaseHelper.getFilteredSalesTotal(currentUserId, startDate, endDate);
        int totalTxns = databaseHelper.getFilteredSalesCount(currentUserId, startDate, endDate);
        int totalUnits = databaseHelper.getFilteredUnitsSold(currentUserId, startDate, endDate);
        double avgOrderValue = (totalTxns > 0) ? (totalSales / totalTxns) : 0.0;

        if (tvTotalSales != null) tvTotalSales.setText(String.format("₱%.2f", totalSales));
        if (tvTotalTransactions != null) tvTotalTransactions.setText(totalTxns + "");
        if (tvUnitsSold != null) tvUnitsSold.setText(totalUnits + "");
        if (tvAvgOrderValue != null) tvAvgOrderValue.setText(String.format("₱%.2f", avgOrderValue));

        // Load Sales Trend Chart
        List<DailySalesData> chartData = databaseHelper.getDailySalesTrend(currentUserId, startDate, endDate);
        if (chartSalesTrend != null) {
            chartSalesTrend.setDataPoints(chartData);
        }

        // Load Product Performance / Best Sellers
        List<BestSellerItem> bestSellers = databaseHelper.getFilteredBestSellers(currentUserId, startDate, endDate);

        if (totalTxns == 0 && totalSales == 0.0) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            if (layoutActiveState != null) layoutActiveState.setVisibility(View.GONE);
        } else {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            if (layoutActiveState != null) layoutActiveState.setVisibility(View.VISIBLE);

            if (rvBestSellers != null) {
                BestSellerAdapter adapter = new BestSellerAdapter(this, bestSellers);
                rvBestSellers.setAdapter(adapter);
            }
        }
    }

    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    private String getPastDateString(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(cal.getTime());
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