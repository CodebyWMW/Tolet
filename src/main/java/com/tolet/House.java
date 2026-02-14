package com.tolet;

public class House {
    private String location;
    private String type;
    private double rent;
    private String ownerName;

    // New fields for the card view
    private String image;
    private int bedrooms;
    private int bathrooms;
    private double area;

    public House(String location, String type, double rent, String ownerName, String image, int bedrooms, int bathrooms,
            double area) {
        this.location = location;
        this.type = type;
        this.rent = rent;
        this.ownerName = ownerName;
        this.image = image;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.area = area;
    }

    public String getLocation() {
        return location;
    }

    public String getType() {
        return type;
    }

    public double getRent() {
        return rent;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getImage() {
        return image;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public int getBathrooms() {
        return bathrooms;
    }

    public double getArea() {
        return area;
    }
}