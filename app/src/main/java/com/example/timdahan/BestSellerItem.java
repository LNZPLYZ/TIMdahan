package com.example.timdahan;

public class BestSellerItem {
    private String productName;
    private int totalUnitsSold;
    private double totalRevenue;

    public BestSellerItem(String productName, int totalUnitsSold, double totalRevenue) {
        this.productName = productName;
        this.totalUnitsSold = totalUnitsSold;
        this.totalRevenue = totalRevenue;
    }

    public String getProductName() {
        return productName;
    }

    public int getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }
}