package com.example.timdahan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        default void onEditClick(Product product) { onProductClick(product); }
        default void onDeleteClick(Product product) {}
    }

    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    public void setFilteredList(List<Product> filteredList) {
        this.productList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvName.setText(product.getName());
        holder.tvInfo.setText(product.getCategory() + " | Price: ₱" + String.format("%.2f", product.getPrice()));
        holder.tvStock.setText(product.getStockQuantity() + " left");

        // Stock status badge check
        if (product.getStockQuantity() <= 0) {
            holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.error_text));
            holder.tvStatus.setText("Out of stock");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error_text));
            holder.tvStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.error_bg_tint));
        } else if (product.getStockQuantity() <= product.getMinimumStock()) {
            holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.error_text));
            holder.tvStatus.setText("Low stock");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error_text));
            holder.tvStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.error_bg_tint));
        } else {
            holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
            holder.tvStatus.setText("In stock");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success_text));
            holder.tvStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.success_bg_tint));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            }
        });

        if (holder.btnEdit != null) {
            holder.btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onEditClick(product);
                    }
                }
            });
        }

        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onDeleteClick(product);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo, tvStock, tvStatus;
        ImageView btnEdit, btnDelete;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvInfo = itemView.findViewById(R.id.tv_product_info);
            tvStock = itemView.findViewById(R.id.tv_product_stock);
            tvStatus = itemView.findViewById(R.id.tv_stock_status);
            btnEdit = itemView.findViewById(R.id.btn_edit_product);
            btnDelete = itemView.findViewById(R.id.btn_delete_product);
        }
    }
}