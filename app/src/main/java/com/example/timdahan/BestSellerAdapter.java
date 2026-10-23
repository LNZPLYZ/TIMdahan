package com.example.timdahan;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BestSellerAdapter extends RecyclerView.Adapter<BestSellerAdapter.ViewHolder> {

    private Context context;
    private List<BestSellerItem> bestSellerList;

    public BestSellerAdapter(Context context, List<BestSellerItem> bestSellerList) {
        this.context = context;
        this.bestSellerList = bestSellerList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_best_seller, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BestSellerItem item = bestSellerList.get(position);
        int rank = position + 1;

        holder.tvRank.setText(String.valueOf(rank));
        holder.tvName.setText(item.getProductName());
        holder.tvCount.setText(item.getTotalUnitsSold() + " units sold • ₱" + String.format("%.2f", item.getTotalRevenue()));

        // Styling based on rank
        if (rank == 1) {
            holder.tvRank.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
            holder.tvRank.setTextColor(Color.parseColor("#D97706"));
        } else if (rank == 2) {
            holder.tvRank.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2FE")));
            holder.tvRank.setTextColor(Color.parseColor("#0284C7"));
        } else {
            holder.tvRank.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
            holder.tvRank.setTextColor(Color.parseColor("#4B5563"));
        }
    }

    @Override
    public int getItemCount() {
        return bestSellerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvName = itemView.findViewById(R.id.tv_best_seller_name);
            tvCount = itemView.findViewById(R.id.tv_best_seller_count);
        }
    }
}