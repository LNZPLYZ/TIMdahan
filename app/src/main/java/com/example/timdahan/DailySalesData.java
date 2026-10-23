package com.example.timdahan;

public class DailySalesData {
    private String dateLabel; // e.g. "Feb 23"
    private double totalSales; // e.g. 150.00

    public DailySalesData(String dateLabel, double totalSales) {
        this.dateLabel = dateLabel;
        this.totalSales = totalSales;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public double getTotalSales() {
        return totalSales;
    }
}