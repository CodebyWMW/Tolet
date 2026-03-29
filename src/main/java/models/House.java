package models;

public class House {

    private int id;
    private int ownerId;
    private String ownerPublicId;
    private String location;

    private boolean familyAllowed;
    private boolean bachelorAllowed;
    private boolean gasAvailable;
    private boolean parkingAvailable;
    private boolean furnished;
    private boolean petFriendly;

    private String title;          // NEW
    private double rent;           // NEW
    private String image;
    private int bedrooms;
    private int bathrooms;
    private double area;

    private String approvalStatus; // pending, approved, rejected

    // =============================
    // Empty constructor
    // =============================
    public House() {
    }

    // =============================
    // Constructor without id
    // =============================
    public House(int ownerId,
                 String location,
                 boolean familyAllowed,
                 boolean bachelorAllowed,
                 boolean gasAvailable,
                 boolean parkingAvailable,
                 boolean furnished,
                 boolean petFriendly,
                 String title,
                 double rent) {

        this.ownerId = ownerId;
        this.location = location;
        this.familyAllowed = familyAllowed;
        this.bachelorAllowed = bachelorAllowed;
        this.gasAvailable = gasAvailable;
        this.parkingAvailable = parkingAvailable;
        this.furnished = furnished;
        this.petFriendly = petFriendly;
        this.title = title;
        this.rent = rent;
    }

    // =============================
    // Getters and Setters
    // =============================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerPublicId() {
        return ownerPublicId;
    }

    public void setOwnerPublicId(String ownerPublicId) {
        this.ownerPublicId = ownerPublicId;
    }

    public String getOwnerDisplayId() {
        if (ownerPublicId != null && !ownerPublicId.isBlank()) {
            return ownerPublicId;
        }
        return String.valueOf(ownerId);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isFamilyAllowed() {
        return familyAllowed;
    }

    public void setFamilyAllowed(boolean familyAllowed) {
        this.familyAllowed = familyAllowed;
    }

    public boolean isBachelorAllowed() {
        return bachelorAllowed;
    }

    public void setBachelorAllowed(boolean bachelorAllowed) {
        this.bachelorAllowed = bachelorAllowed;
    }

    public boolean isGasAvailable() {
        return gasAvailable;
    }

    public void setGasAvailable(boolean gasAvailable) {
        this.gasAvailable = gasAvailable;
    }

    public boolean isParkingAvailable() {
        return parkingAvailable;
    }

    public void setParkingAvailable(boolean parkingAvailable) {
        this.parkingAvailable = parkingAvailable;
    }

    public boolean isFurnished() {
        return furnished;
    }

    public void setFurnished(boolean furnished) {
        this.furnished = furnished;
    }

    public boolean isPetFriendly() {
        return petFriendly;
    }

    public void setPetFriendly(boolean petFriendly) {
        this.petFriendly = petFriendly;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getRent() {
        return rent;
    }

    public void setRent(double rent) {
        this.rent = rent;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(int bedrooms) {
        this.bedrooms = bedrooms;
    }

    public int getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(int bathrooms) {
        this.bathrooms = bathrooms;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }
}