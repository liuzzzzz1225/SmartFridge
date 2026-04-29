package com.example.myapplication;

public class Food {
    private int id;
    private String name;
    private String expiryDate;
    private int quantity;

    public Food(String name, String expiryDate, int quantity) {
        this.name = name;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
    }

    public Food(int id, String name, String expiryDate, int quantity) {
        this.id = id;
        this.name = name;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public int getQuantity() {
        return quantity;
    }
} 