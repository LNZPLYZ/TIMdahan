package com.example.timdahan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private Context context;
    private List<SaleTransaction> transactionList;
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(SaleTransaction transaction);
    }

    public TransactionAdapter(Context context, List<SaleTransaction> transactionList, OnTransactionClickListener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = listener;
    }

    public void setFilteredList(List<SaleTransaction> filteredList) {
        this.transactionList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SaleTransaction transaction = transactionList.get(position);

        holder.tvId.setText("Transaction #" + transaction.getTransactionNumber());
        holder.tvDate.setText(transaction.getSaleDate());
        holder.tvAmount.setText(String.format("₱%.2f", transaction.getTotalAmount()));

        int qty = transaction.getTotalItemsQuantity();
        holder.tvQuantity.setText(qty + (qty == 1 ? " item" : " items"));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onTransactionClick(transaction);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvDate, tvAmount, tvQuantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tv_transaction_id);
            tvDate = itemView.findViewById(R.id.tv_transaction_date);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvQuantity = itemView.findViewById(R.id.tv_transaction_quantity);
        }
    }
}