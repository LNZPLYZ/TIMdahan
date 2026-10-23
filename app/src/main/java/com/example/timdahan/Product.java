package com.example.timdahan;

public class Product {
    private int id;
    private String barcode;
    private String name;
    private String category;
    private double price;
    private int stockQuantity;
    private int minimumStock;

    // Empty Constructor
    public Product() {
    }

    // Constructor with parameters (without id, used for creating a new product)
    public Product(String barcode, String name, String category, double price, int stockQuantity, int minimumStock) {
        this.barcode = barcode;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.minimumStock = minimumStock;
    }

    // Full Constructor (with id, used when loading existing product from SQLite)
    public Product(int id, String barcode, String name, String category, double price, int stockQuantity, int minimumStock) {
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.minimumStock = minimumStock;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }
}