package com.tolet;

public class House {
    private String location;
    private String type; // Family, Bachelor, Office
    private double rent;
    private String ownerName;

    public House(String location, String type, double rent, String ownerName) {
        this.location = location;
        this.type = type;
        this.rent = rent;
        this.ownerName = ownerName;
    }

    // Getters are required for TableView
    public String getLocation() { return location; }
    public String getType() { return type; }
    public double getRent() { return rent; }
    public String getOwnerName() { return ownerName; }
    
    @Override
    public String toString() { return location + " (" + type + ")"; }
}