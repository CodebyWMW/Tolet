package com.tolet;

import database.DatabaseConnection;
import database.TableCreator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import java.sql.*;

public class DataStore {
    public static User currentUser;
    public static boolean darkMode = false;

    public static void initData() {
        TableCreator.createTables();
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        String darkThemeUrl = DataStore.class.getResource("/com/tolet/dark-theme.css").toExternalForm();
        if (darkMode) {
            if (!scene.getStylesheets().contains(darkThemeUrl)) scene.getStylesheets().add(darkThemeUrl);
        } else {
            scene.getStylesheets().remove(darkThemeUrl);
        }
    }

    // --- NEW: FETCH HOUSES FROM DB ---
    public static ObservableList<House> getHouses() {
        ObservableList<House> list = FXCollections.observableArrayList();
        // Joins houses table with users table to get the Owner's Name
        String query = "SELECT h.address, h.type, h.rent, u.name FROM houses h JOIN users u ON h.owner_id = u.id";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                list.add(new House(
                    rs.getString("address"),
                    rs.getString("type"),
                    rs.getDouble("rent"),
                    rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- NEW: ADD HOUSE TO DB ---
    public static void addHouse(String location, String type, double rent) {
        int ownerId = getUserId(currentUser.getEmail()); // Helper to find ID
        if (ownerId == -1) return;

        // Note: We default 'city' to Dhaka for now to satisfy your table schema
        String query = "INSERT INTO houses (address, city, type, rent, owner_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, location);
            pstmt.setString(2, "Dhaka"); 
            pstmt.setString(3, type);
            pstmt.setDouble(4, rent);
            pstmt.setInt(5, ownerId);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- HELPER: GET USER ID ---
    private static int getUserId(String email) {
        String query = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // --- USER AUTH METHODS (Existing) ---
    public static boolean validateUser(String emailOrPhone, String password) {
        String query = "SELECT * FROM users WHERE (email = ? OR phone = ?) AND password = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, emailOrPhone);
            pstmt.setString(2, emailOrPhone);
            pstmt.setString(3, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentUser = new User(rs.getString("name"), rs.getString("email"), rs.getString("password"), rs.getString("role"));
                return true;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean registerUser(String name, String email, String password, String role) {
        String query = "INSERT INTO users (name, email, password, role, phone) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            if (email.contains("@")) { pstmt.setString(5, null); } 
            else { pstmt.setString(2, "temp_" + System.currentTimeMillis()); pstmt.setString(5, email); }
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    
    public static boolean emailExists(String email) {
        String query = "SELECT email FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email); return pstmt.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public static void updatePassword(String email, String newPass) {
        String query = "UPDATE users SET password = ? WHERE email = ?";
        try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newPass); pstmt.setString(2, email); pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}