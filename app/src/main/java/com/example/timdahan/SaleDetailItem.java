package com.example.timdahan;

public class SaleDetailItem {
    private String productName;
    private int quantity;
    private double unitPrice;
    private double subtotal;

    public SaleDetailItem(String productName, int quantity, double unitPrice, double subtotal) {
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getSubtotal() {
        return subtotal;
    }
}