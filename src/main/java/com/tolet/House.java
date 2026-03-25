package com.tolet;

public class House {
    private int id;
    private String title;
    private String location;
    private String type;
    private double rent;
    private String ownerName;
    private String approvalStatus;

    // New fields for the card view
    private String image;
    private int bedrooms;
    private int bathrooms;
    private double area;

    public House(int id, String title, String location, String type, double rent, String ownerName, String image, int bedrooms,
            int bathrooms,
            double area) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.type = type;
        this.rent = rent;
        this.ownerName = ownerName;
        this.approvalStatus = "";
        this.image = image;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.area = area;
    }

    public House(String title, String location, String type, double rent, String ownerName, String image, int bedrooms, int bathrooms,
            double area) {
        this(-1, title, location, type, rent, ownerName, image, bedrooms, bathrooms, area);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
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

    public String getApprovalStatus() {
        return approvalStatus;
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

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
}