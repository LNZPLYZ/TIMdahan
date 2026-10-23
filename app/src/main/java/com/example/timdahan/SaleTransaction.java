package com.example.timdahan;

public class SaleTransaction {
    private int saleId;
    private int userId;
    private int transactionNumber;
    private String saleDate;
    private double totalAmount;
    private int totalItemsQuantity;

    public SaleTransaction(int saleId, int userId, int transactionNumber, String saleDate, double totalAmount, int totalItemsQuantity) {
        this.saleId = saleId;
        this.userId = userId;
        this.transactionNumber = transactionNumber;
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;
        this.totalItemsQuantity = totalItemsQuantity;
    }

    public int getSaleId() {
        return saleId;
    }

    public int getUserId() {
        return userId;
    }

    public int getTransactionNumber() {
        return transactionNumber > 0 ? transactionNumber : saleId;
    }

    public String getSaleDate() {
        return saleDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getTotalItemsQuantity() {
        return totalItemsQuantity;
    }
}