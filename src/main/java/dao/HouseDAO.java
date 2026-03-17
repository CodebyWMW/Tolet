package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import database.DatabaseConnection;
import models.House;

public class HouseDAO {

    // =============================
    // Utility Method (Avoid Duplication)
    // =============================
    private House extractHouse(ResultSet rs) throws SQLException {
        House house = new House();

        house.setId(safeInt(rs, "id", 0));
        house.setOwnerId(safeInt(rs, "owner_id", 0));
        house.setOwnerPublicId(safeString(rs, "owner_public_id", ""));
        house.setLocation(safeString(rs, "location", "-"));
        house.setFamilyAllowed(safeInt(rs, "family_allowed", 0) == 1);
        house.setBachelorAllowed(safeInt(rs, "bachelor_allowed", 0) == 1);
        house.setGasAvailable(safeInt(rs, "gas_available", 0) == 1);
        house.setParkingAvailable(safeInt(rs, "parking_available", 0) == 1);
        house.setFurnished(safeInt(rs, "furnished", 0) == 1);
        house.setPetFriendly(safeInt(rs, "pet_friendly", 0) == 1);
        house.setTitle(safeString(rs, "title", ""));
        house.setRent(safeDouble(rs, "rent", 0.0));
        house.setApprovalStatus(safeString(rs, "approval_status", "pending"));

        return house;
    }

    private String safeString(ResultSet rs, String column, String fallback) {
        try {
            String value = rs.getString(column);
            return value == null ? fallback : value;
        } catch (SQLException e) {
            return fallback;
        }
    }

    private int safeInt(ResultSet rs, String column, int fallback) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return fallback;
        }
    }

    private double safeDouble(ResultSet rs, String column, double fallback) {
        try {
            return rs.getDouble(column);
        } catch (SQLException e) {
            return fallback;
        }
    }

    // =============================
    // Get All Approved Houses (Client View)
    // =============================
    public List<House> getAllHouses() {
        List<House> houses = new ArrayList<>();
        String sql = "SELECT * FROM houses WHERE approval_status = 'approved'";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                houses.add(extractHouse(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return houses;
    }

    // =============================
    // ADMIN: Get All Listings
    // =============================
    public List<House> getAllListingsForAdmin() {
        List<House> houses = new ArrayList<>();
        String sql = "SELECT h.*, u.public_id AS owner_public_id "
            + "FROM houses h "
            + "LEFT JOIN users u ON u.id = h.owner_id "
            + "ORDER BY h.id DESC";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                houses.add(extractHouse(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return houses;
    }

    // =============================
    // Add House (Owner Request → Pending)
    // =============================
    public boolean addHouse(House house) {
        String sql = "INSERT INTO houses (location, owner_id, family_allowed, bachelor_allowed, gas_available, parking_available, furnished, pet_friendly, title, rent, approval_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, house.getLocation());
            pstmt.setInt(2, house.getOwnerId());
            pstmt.setInt(3, house.isFamilyAllowed() ? 1 : 0);
            pstmt.setInt(4, house.isBachelorAllowed() ? 1 : 0);
            pstmt.setInt(5, house.isGasAvailable() ? 1 : 0);
            pstmt.setInt(6, house.isParkingAvailable() ? 1 : 0);
            pstmt.setInt(7, house.isFurnished() ? 1 : 0);
            pstmt.setInt(8, house.isPetFriendly() ? 1 : 0);
            pstmt.setString(9, house.getTitle());
            pstmt.setDouble(10, house.getRent());
            pstmt.setString(11, "pending");

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =============================
    // Get Houses by Owner
    // =============================
    public List<House> getHousesByOwnerId(int ownerId) {
        List<House> houses = new ArrayList<>();
        String sql = "SELECT * FROM houses WHERE owner_id = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ownerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                houses.add(extractHouse(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return houses;
    }

    // =============================
    // ADMIN: Get Houses by Status
    // =============================
    public List<House> getHousesByStatus(String status) {
        List<House> houses = new ArrayList<>();
        String sql = "SELECT * FROM houses WHERE approval_status = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                houses.add(extractHouse(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return houses;
    }

    // =============================
    // ADMIN: Update Approval Status
    // =============================
    public boolean updateHouseStatus(int houseId, String status) {
        String sql = "UPDATE houses SET approval_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, houseId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =============================
    // Get Single House by ID
    // =============================
    public House getHouseById(int houseId) {
        String sql = "SELECT * FROM houses WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, houseId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractHouse(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // =============================
    // OWNER: Update House Details
    // =============================
    public boolean updateHouseDetails(int houseId, int ownerId, String title, String shortDetail, String location,
            String details, String tags, String availability, String type, int bedrooms, int bathrooms, double area,
            boolean gasAvailable, boolean waterAvailable, boolean currentAvailable, double rent, String contactInfo,
            boolean familyAllowed, boolean bachelorAllowed) {
        String sql = "UPDATE houses SET title = ?, short_detail = ?, location = ?, details = ?, tags = ?, availability = ?, "
                + "type = ?, bedrooms = ?, bathrooms = ?, area = ?, gas_available = ?, water_available = ?, current_available = ?, "
                + "rent = ?, contact_info = ?, family_allowed = ?, bachelor_allowed = ? "
                + "WHERE id = ? AND owner_id = ?";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, shortDetail);
            pstmt.setString(3, location);
            pstmt.setString(4, details);
            pstmt.setString(5, tags);
            pstmt.setString(6, availability);
            pstmt.setString(7, type);
            pstmt.setInt(8, bedrooms);
            pstmt.setInt(9, bathrooms);
            pstmt.setDouble(10, area);
            pstmt.setInt(11, gasAvailable ? 1 : 0);
            pstmt.setInt(12, waterAvailable ? 1 : 0);
            pstmt.setInt(13, currentAvailable ? 1 : 0);
            pstmt.setDouble(14, rent);
            pstmt.setString(15, contactInfo);
            pstmt.setInt(16, familyAllowed ? 1 : 0);
            pstmt.setInt(17, bachelorAllowed ? 1 : 0);
            pstmt.setInt(18, houseId);
            pstmt.setInt(19, ownerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =============================
    // OWNER: Delete House Listing
    // =============================
    public boolean deleteHouseById(int houseId, int ownerId) {
        String deleteRequests = "DELETE FROM rent_requests WHERE house_id = ?";
        String deleteImages = "DELETE FROM house_images WHERE house_id = ?";
        String deleteHouse = "DELETE FROM houses WHERE id = ? AND owner_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement requestsStmt = conn.prepareStatement(deleteRequests);
                    PreparedStatement imagesStmt = conn.prepareStatement(deleteImages);
                    PreparedStatement houseStmt = conn.prepareStatement(deleteHouse)) {
                requestsStmt.setInt(1, houseId);
                requestsStmt.executeUpdate();

                imagesStmt.setInt(1, houseId);
                imagesStmt.executeUpdate();

                houseStmt.setInt(1, houseId);
                houseStmt.setInt(2, ownerId);
                int updated = houseStmt.executeUpdate();

                conn.commit();
                return updated > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}