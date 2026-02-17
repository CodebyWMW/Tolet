package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import database.DatabaseConnection;
import models.House;

public class HouseDAO {

    public List<House> getAllHouses() {
        List<House> houses = new ArrayList<>();

        String sql = "SELECT * FROM houses";

        try (Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                House house = new House();

                house.setId(rs.getInt("id"));
                house.setOwnerId(rs.getInt("owner_id"));
                house.setLocation(rs.getString("location"));
                house.setFamilyAllowed(rs.getInt("family_allowed") == 1);
                house.setBachelorAllowed(rs.getInt("bachelor_allowed") == 1);
                house.setGasAvailable(rs.getInt("gas_available") == 1);
                house.setParkingAvailable(rs.getInt("parking_available") == 1);
                house.setFurnished(rs.getInt("furnished") == 1);
                house.setPetFriendly(rs.getInt("pet_friendly") == 1);

                try {
                    house.setApprovalStatus(rs.getString("approval_status"));
                } catch (SQLException e) {
                    house.setApprovalStatus("approved");
                }

                houses.add(house);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return houses;
    }

    public void addHouse(House house) {
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
            pstmt.setString(9, "House at " + house.getLocation());
            pstmt.setDouble(10, 15000 + Math.random() * 50000);
            pstmt.setString(11, "pending"); // New houses need approval

            pstmt.executeUpdate();
            System.out.println("House added successfully: " + house.getLocation());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<House> getHousesByOwnerId(int ownerId) {
        List<House> houses = new ArrayList<>();
        String sql = "SELECT * FROM houses WHERE owner_id = ?";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ownerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                House house = new House();
                house.setId(rs.getInt("id"));
                house.setOwnerId(rs.getInt("owner_id"));
                house.setLocation(rs.getString("location"));
                house.setFamilyAllowed(rs.getInt("family_allowed") == 1);
                house.setBachelorAllowed(rs.getInt("bachelor_allowed") == 1);
                house.setGasAvailable(rs.getInt("gas_available") == 1);
                house.setParkingAvailable(rs.getInt("parking_available") == 1);
                house.setFurnished(rs.getInt("furnished") == 1);
                house.setPetFriendly(rs.getInt("pet_friendly") == 1);
                try {
                    house.setApprovalStatus(rs.getString("approval_status"));
                } catch (SQLException e) {
                    house.setApprovalStatus("approved");
                }

                houses.add(house);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return houses;
    }

    // ADMIN: Get houses by approval status
    public List<House> getHousesByStatus(String status) {
        List<House> houses = new ArrayList<>();
        String sql = "SELECT * FROM houses WHERE approval_status = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                House house = new House();
                house.setId(rs.getInt("id"));
                house.setOwnerId(rs.getInt("owner_id"));
                house.setLocation(rs.getString("location"));
                house.setFamilyAllowed(rs.getInt("family_allowed") == 1);
                house.setBachelorAllowed(rs.getInt("bachelor_allowed") == 1);
                house.setGasAvailable(rs.getInt("gas_available") == 1);
                house.setParkingAvailable(rs.getInt("parking_available") == 1);
                house.setFurnished(rs.getInt("furnished") == 1);
                house.setPetFriendly(rs.getInt("pet_friendly") == 1);
                house.setApprovalStatus(rs.getString("approval_status"));

                houses.add(house);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return houses;
    }

    // ADMIN: Update house approval status
    public boolean updateHouseStatus(int houseId, String status) {
        String sql = "UPDATE houses SET approval_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, houseId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ADMIN: Get house with owner info
    public House getHouseWithOwner(int houseId) {
        String sql = "SELECT * FROM houses WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, houseId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                House house = new House();
                house.setId(rs.getInt("id"));
                house.setOwnerId(rs.getInt("owner_id"));
                house.setLocation(rs.getString("location"));
                house.setFamilyAllowed(rs.getInt("family_allowed") == 1);
                house.setBachelorAllowed(rs.getInt("bachelor_allowed") == 1);
                house.setGasAvailable(rs.getInt("gas_available") == 1);
                house.setParkingAvailable(rs.getInt("parking_available") == 1);
                house.setFurnished(rs.getInt("furnished") == 1);
                house.setPetFriendly(rs.getInt("pet_friendly") == 1);
                try {
                    house.setApprovalStatus(rs.getString("approval_status"));
                } catch (SQLException e) {
                    house.setApprovalStatus("approved");
                }
                return house;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}