package com.example.timdahan;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private UserSession userSession;
    private int currentUserId;

    private RecyclerView rvTransactions;
    private LinearLayout layoutEmptyState;
    private EditText etSearch;

    private TransactionAdapter adapter;
    private List<SaleTransaction> transactionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        hideNavigationBar();

        databaseHelper = new DatabaseHelper(this);
        userSession = new UserSession(this);
        currentUserId = userSession.getUserId();

        ImageView btnBack = findViewById(R.id.btn_back_history);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvTransactions = findViewById(R.id.rv_transactions);
        layoutEmptyState = findViewById(R.id.layout_empty_transactions);
        etSearch = findViewById(R.id.et_search_transactions);

        if (rvTransactions != null) {
            rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        }

        Button btnRecordSaleEmpty = findViewById(R.id.btn_record_sale_empty_txn);
        if (btnRecordSaleEmpty != null) {
            btnRecordSaleEmpty.setOnClickListener(v -> startActivity(new Intent(TransactionHistoryActivity.this, RecordSaleActivity.class)));
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterTransactions(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        loadTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void loadTransactions() {
        transactionList = databaseHelper.getAllTransactions(currentUserId);

        if (transactionList.isEmpty()) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            if (rvTransactions != null) rvTransactions.setVisibility(View.GONE);
        } else {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            if (rvTransactions != null) rvTransactions.setVisibility(View.VISIBLE);

            adapter = new TransactionAdapter(this, transactionList, new TransactionAdapter.OnTransactionClickListener() {
                @Override
                public void onTransactionClick(SaleTransaction transaction) {
                    showTransactionDetailsDialog(transaction);
                }
            });
            if (rvTransactions != null) {
                rvTransactions.setAdapter(adapter);
            }
        }
    }

    private void filterTransactions(String query) {
        if (transactionList == null) return;

        List<SaleTransaction> filteredList = new ArrayList<>();
        for (SaleTransaction txn : transactionList) {
            String txnIdStr = String.valueOf(txn.getTransactionNumber());
            if (txnIdStr.contains(query) || txn.getSaleDate().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(txn);
            }
        }

        if (adapter != null) {
            adapter.setFilteredList(filteredList);
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

        // Query historical sale items from SQLite
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

    private void hideNavigationBar() {
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
    }
}