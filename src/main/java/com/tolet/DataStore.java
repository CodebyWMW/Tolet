package com.tolet;

import database.DatabaseConnection;
import database.TableCreator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.sql.*;
import java.time.LocalDate;

public class DataStore {
    public static User currentUser;
    public static boolean darkMode = false;

    public static void initData() {
        TableCreator.createTables();

        // Populate dummy data if empty (for testing)
        if (getHouses().isEmpty()) {
            addMockData();
        }

        addMockRequestsIfEmpty();
    }

    public static void applyTheme(Scene scene) {
        if (scene == null)
            return;
        String darkThemeUrl = DataStore.class.getResource("/com/tolet/dark-theme.css").toExternalForm();
        if (darkMode) {
            if (!scene.getStylesheets().contains(darkThemeUrl))
                scene.getStylesheets().add(darkThemeUrl);
        } else {
            scene.getStylesheets().remove(darkThemeUrl);
        }
    }

    public static void applyWindowSize(Stage stage) {
        if (stage == null)
            return;
        stage.setResizable(true);
    }

    public static String resolveFxml(String baseFxml) {
        if (baseFxml == null || baseFxml.isBlank()) {
            return baseFxml;
        }
        if (darkMode) {
            if (baseFxml.endsWith("-dark.fxml")) {
                return baseFxml;
            }
            if (baseFxml.endsWith(".fxml")) {
                return baseFxml.replace(".fxml", "-dark.fxml");
            }
        } else if (baseFxml.endsWith("-dark.fxml")) {
            return baseFxml.replace("-dark.fxml", ".fxml");
        }
        return baseFxml;
    }

    public static ObservableList<House> getHouses() {
        ObservableList<House> list = FXCollections.observableArrayList();
        String query = "SELECT h.address, h.type, h.rent, h.image, h.bedrooms, h.bathrooms, h.area, u.name " +
                "FROM houses h JOIN users u ON h.owner_id = u.id";

        try (Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                list.add(new House(
                        rs.getString("address"),
                        rs.getString("type"),
                        rs.getDouble("rent"),
                        rs.getString("name"),
                        rs.getString("image"),
                        rs.getInt("bedrooms"),
                        rs.getInt("bathrooms"),
                        rs.getDouble("area")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static ObservableList<BookingRequest> getBookingRequests() {
        ObservableList<BookingRequest> list = FXCollections.observableArrayList();
        Integer ownerId = getCurrentUserId();
        boolean filterOwner = currentUser != null
                && currentUser.getRole() != null
                && currentUser.getRole().toLowerCase().contains("owner")
                && ownerId != null;

        String query = "SELECT u.name AS tenant_name, h.address, r.request_date, r.move_in_date, h.rent, r.status "
                + "FROM rent_requests r "
                + "JOIN users u ON r.tenant_id = u.id "
                + "JOIN houses h ON r.house_id = h.id";
        if (filterOwner) {
            query += " WHERE h.owner_id = ?";
        }

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            if (filterOwner) {
                pstmt.setInt(1, ownerId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new BookingRequest(
                        rs.getString("tenant_name"),
                        rs.getString("address"),
                        parseDate(rs.getString("request_date")),
                        parseDate(rs.getString("move_in_date")),
                        rs.getDouble("rent"),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Temporary helper to add the React mock data into DB
    private static void addMockData() {
        // We need a dummy owner first
        registerUser("System Owner", "sys@owner.com", "123", "House Owner");
        int ownerId = 1; // Assuming first user

        String sql = "INSERT INTO houses (address, city, type, rent, owner_id, image, bedrooms, bathrooms, area) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Object[][] mockData = {
                    { "Gulshan 2, Dhaka", "Dhaka", "Family", 45000, ownerId,
                            "https://images.unsplash.com/photo-1594873604892-b599f847e859?w=400", 3, 2, 1500 },
                    { "Banani, Dhaka", "Dhaka", "Bachelor", 18000, ownerId,
                            "https://images.unsplash.com/photo-1706808849780-7a04fbac83ef?w=400", 1, 1, 650 },
                    { "Dhanmondi, Dhaka", "Dhaka", "Family", 28000, ownerId,
                            "https://images.unsplash.com/photo-1612419299101-6c294dc2901d?w=400", 2, 1, 1000 },
                    { "Uttara, Dhaka", "Dhaka", "Family", 65000, ownerId,
                            "https://images.unsplash.com/photo-1760561148422-bbb515696fb7?w=400", 4, 3, 2200 }
            };

            for (Object[] row : mockData) {
                pstmt.setString(1, (String) row[0]);
                pstmt.setString(2, (String) row[1]);
                pstmt.setString(3, (String) row[2]);
                pstmt.setDouble(4, (Integer) row[3]);
                pstmt.setInt(5, (Integer) row[4]);
                pstmt.setString(6, (String) row[5]);
                pstmt.setInt(7, (Integer) row[6]);
                pstmt.setInt(8, (Integer) row[7]);
                pstmt.setDouble(9, (Integer) row[8]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addMockRequestsIfEmpty() {
        if (countRows("rent_requests") > 0) {
            return;
        }

        Integer ownerId = getUserIdByRole("House Owner");
        if (ownerId == null) {
            registerUser("System Owner", "sys@owner.com", "123", "House Owner");
            ownerId = getUserIdByRole("House Owner");
        }

        Integer tenantId = getUserIdByRole("Tenant");
        if (tenantId == null) {
            registerUser("System Tenant", "tenant@demo.com", "123", "Tenant");
            tenantId = getUserIdByRole("Tenant");
        }

        Integer houseId = getFirstHouseIdByOwner(ownerId);
        if (houseId == null) {
            return;
        }

        String sql = "INSERT INTO rent_requests (house_id, tenant_id, request_date, move_in_date, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            addRequestBatch(pstmt, houseId, tenantId,
                    LocalDate.of(2026, 2, 5), LocalDate.of(2026, 3, 1), "Pending");
            addRequestBatch(pstmt, houseId, tenantId,
                    LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 15), "Approved");
            addRequestBatch(pstmt, houseId, tenantId,
                    LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 20), "Denied");
            pstmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addRequestBatch(PreparedStatement pstmt, int houseId, int tenantId,
            LocalDate requestDate, LocalDate moveInDate, String status) throws SQLException {
        pstmt.setInt(1, houseId);
        pstmt.setInt(2, tenantId);
        pstmt.setString(3, requestDate != null ? requestDate.toString() : null);
        pstmt.setString(4, moveInDate != null ? moveInDate.toString() : null);
        pstmt.setString(5, status);
        pstmt.addBatch();
    }

    private static Integer getFirstHouseIdByOwner(Integer ownerId) {
        if (ownerId == null) {
            return null;
        }
        String query = "SELECT id FROM houses WHERE owner_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, ownerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Integer getCurrentUserId() {
        if (currentUser == null) {
            return null;
        }
        String query = "SELECT id FROM users WHERE email = ? OR name = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, currentUser.getEmail());
            pstmt.setString(2, currentUser.getUsername());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Integer getUserIdByRole(String role) {
        String query = "SELECT id FROM users WHERE role = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int countRows(String tableName) {
        String query = "SELECT COUNT(*) AS total FROM " + tableName;
        try (Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    // --- KEEP YOUR EXISTING AUTH METHODS BELOW (validateUser, registerUser, etc)
    // ---
    public static boolean validateUser(String emailOrPhone, String password) {
        String query = "SELECT * FROM users WHERE (email = ? OR phone = ?) AND password = ?";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, emailOrPhone);
            pstmt.setString(2, emailOrPhone);
            pstmt.setString(3, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentUser = new User(rs.getString("name"), rs.getString("email"), rs.getString("password"),
                        rs.getString("role"));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            if (email.contains("@")) {
                pstmt.setString(5, null);
            } else {
                pstmt.setString(2, "temp_" + System.currentTimeMillis());
                pstmt.setString(5, email);
            }
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean emailExists(String email) {
        String query = "SELECT email FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void updatePassword(String email, String newPass) {
        String query = "UPDATE users SET password = ? WHERE email = ?";
        try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newPass);
            pstmt.setString(2, email);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Stub for addHouse used by OwnerController - update this later to match new
    // schema if needed
    public static void addHouse(String location, String type, double rent) {
        // For now just insert basic info, null for others
        int ownerId = 1; // simplistic
        if (currentUser != null) {
            /* get ID logic */ }
        String query = "INSERT INTO houses (address, city, type, rent, owner_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
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
}