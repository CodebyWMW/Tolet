package com.tolet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.prefs.Preferences;

import database.DatabaseConnection;
import database.TableCreator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DataStore {
    public static User currentUser;
    public static boolean darkMode = false;
    private static Double lastWindowWidth;
    private static Double lastWindowHeight;
    private static Double lastWindowX;
    private static Double lastWindowY;
    private static final String WINDOW_TRACKING_KEY = "windowSizeTrackingAttached";
    private static final Preferences PREFS = Preferences.userNodeForPackage(DataStore.class);
    private static final String KEY_KEEP_SIGNED_IN = "keepSignedIn";
    private static final String KEY_SESSION_ROLE = "sessionRole";
    private static final String KEY_SESSION_USERNAME = "sessionUsername";
    private static final String KEY_SESSION_EMAIL = "sessionEmail";
    private static final String KEY_SESSION_MIGRATION_VERSION = "sessionMigrationVersion";
    private static final int CURRENT_SESSION_MIGRATION_VERSION = 1;

    public static void initData() {
        TableCreator.createTables();
        // Populate basic seed data (create a system owner/tenant and sample houses)
        ensureSeedData();
    }

    private static void ensureSeedData() {
        // Create owner and tenant if missing
        if (countRows("users") == 0) {
            registerUser("System Owner", "sys@owner.com", "123", "House Owner");
            registerUser("System Tenant", "tenant@demo.com", "123", "Tenant");
        }

        // Insert sample houses if none exist
        if (countRows("houses") == 0) {
            Integer ownerId = getUserIdByRole("House Owner");
            if (ownerId == null) {
                registerUser("System Owner", "sys@owner.com", "123", "House Owner");
                ownerId = getUserIdByRole("House Owner");
            }

            String sql = "INSERT INTO houses (location, type, rent, owner_id, image, bedrooms, bathrooms, area) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                Object[][] mock = new Object[][] {
                        { "Gulshan 2, Dhaka", "Family", 45000.0, ownerId, "https://images.unsplash.com/photo-1594873604892-b599f847e859?w=400", 3, 2, 1500.0 },
                        { "Banani, Dhaka", "Bachelor", 18000.0, ownerId, "https://images.unsplash.com/photo-1706808849780-7a04fbac83ef?w=400", 1, 1, 650.0 },
                        { "Dhanmondi, Dhaka", "Family", 28000.0, ownerId, "https://images.unsplash.com/photo-1612419299101-6c294dc2901d?w=400", 2, 1, 1000.0 },
                        { "Uttara, Dhaka", "Family", 65000.0, ownerId, "https://images.unsplash.com/photo-1760561148422-bbb515696fb7?w=400", 4, 3, 2200.0 }
                };
                for (Object[] row : mock) {
                    pstmt.setString(1, (String) row[0]);
                    pstmt.setString(2, (String) row[1]);
                    pstmt.setDouble(3, (Double) row[2]);
                    pstmt.setInt(4, (Integer) row[3]);
                    pstmt.setString(5, (String) row[4]);
                    pstmt.setInt(6, (Integer) row[5]);
                    pstmt.setInt(7, (Integer) row[6]);
                    pstmt.setDouble(8, (Double) row[7]);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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

    public static void prepareSceneForRootSwap(Scene scene) {
        if (scene == null) {
            return;
        }

        var styleCss = DataStore.class.getResource("/com/tolet/style.css");
        if (styleCss != null) {
            scene.getStylesheets().remove(styleCss.toExternalForm());
        }
    }

    public static void applyWindowSize(Stage stage) {
        if (stage == null) {
            return;
        }

        stage.setResizable(true);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        if (lastWindowWidth != null && lastWindowHeight != null) {
            double width = Math.min(lastWindowWidth, bounds.getWidth() - 40);
            double height = Math.min(lastWindowHeight, bounds.getHeight() - 40);
            if (width > 0 && height > 0) {
                stage.setWidth(width);
                stage.setHeight(height);
            }
        }

        if (lastWindowX != null && lastWindowY != null) {
            double maxX = bounds.getMaxX() - stage.getWidth();
            double maxY = bounds.getMaxY() - stage.getHeight();
            double clampedX = Math.max(bounds.getMinX(), Math.min(lastWindowX, maxX));
            double clampedY = Math.max(bounds.getMinY(), Math.min(lastWindowY, maxY));
            stage.setX(clampedX);
            stage.setY(clampedY);
        }

        if (Boolean.TRUE.equals(stage.getProperties().get(WINDOW_TRACKING_KEY))) {
            return;
        }

        stage.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.doubleValue() > 0) {
                lastWindowWidth = newValue.doubleValue();
            }
        });

        stage.heightProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.doubleValue() > 0) {
                lastWindowHeight = newValue.doubleValue();
            }
        });

        stage.xProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                lastWindowX = newValue.doubleValue();
            }
        });

        stage.yProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                lastWindowY = newValue.doubleValue();
            }
        });

        stage.getProperties().put(WINDOW_TRACKING_KEY, true);
    }

    public static void rememberWindowState(Stage stage) {
        if (stage == null) {
            return;
        }

        if (stage.getWidth() > 0) {
            lastWindowWidth = stage.getWidth();
        }
        if (stage.getHeight() > 0) {
            lastWindowHeight = stage.getHeight();
        }
        lastWindowX = stage.getX();
        lastWindowY = stage.getY();
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

    public static boolean isKeepSignedInEnabled() {
        return PREFS.getBoolean(KEY_KEEP_SIGNED_IN, false);
    }

    public static void updateRememberedSession(boolean keepSignedIn) {
        if (!keepSignedIn) {
            clearRememberedSession();
            return;
        }

        if (currentUser == null) {
            clearRememberedSession();
            return;
        }

        PREFS.putBoolean(KEY_KEEP_SIGNED_IN, true);
        PREFS.put(KEY_SESSION_ROLE, safeValue(currentUser.getRole()));
        PREFS.put(KEY_SESSION_USERNAME, safeValue(currentUser.getUsername()));
        PREFS.put(KEY_SESSION_EMAIL, safeValue(currentUser.getEmail()));
    }

    public static void clearRememberedSession() {
        PREFS.putBoolean(KEY_KEEP_SIGNED_IN, false);
        PREFS.remove(KEY_SESSION_ROLE);
        PREFS.remove(KEY_SESSION_USERNAME);
        PREFS.remove(KEY_SESSION_EMAIL);
    }

    public static boolean restoreRememberedSession() {
        migrateRememberedSessionIfNeeded();

        if (!isKeepSignedInEnabled()) {
            return false;
        }

        String role = PREFS.get(KEY_SESSION_ROLE, "").trim();
        String username = PREFS.get(KEY_SESSION_USERNAME, "").trim();
        String email = PREFS.get(KEY_SESSION_EMAIL, "").trim();

        if ("admin".equalsIgnoreCase(role)) {
            currentUser = new User("System Admin", "admin@tolet.com", "140945", "Admin");
            return true;
        }

        if (username.isBlank() && email.isBlank()) {
            clearRememberedSession();
            return false;
        }

        String byNameWithRole = "SELECT * FROM users WHERE lower(name) = lower(?) AND lower(ifnull(role, '')) = lower(?) LIMIT 1";
        String byNameOnly = "SELECT * FROM users WHERE lower(name) = lower(?) LIMIT 1";
        String byEmailWithRole = "SELECT * FROM users WHERE lower(ifnull(email, '')) = lower(?) AND lower(ifnull(role, '')) = lower(?) LIMIT 1";
        String byEmailOnly = "SELECT * FROM users WHERE lower(ifnull(email, '')) = lower(?) LIMIT 1";

        try (Connection conn = DatabaseConnection.connect()) {
            // First try username (most stable, especially for phone-based accounts).
            if (!username.isBlank()) {
                String query = role.isBlank() ? byNameOnly : byNameWithRole;
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, username);
                    if (!role.isBlank()) {
                        pstmt.setString(2, role);
                    }
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        currentUser = new User(rs.getString("name"), rs.getString("email"), rs.getString("password"),
                                rs.getString("role"));
                        return true;
                    }
                }
            }

            // Fallback to email only when non-blank.
            if (!email.isBlank()) {
                String query = role.isBlank() ? byEmailOnly : byEmailWithRole;
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, email);
                    if (!role.isBlank()) {
                        pstmt.setString(2, role);
                    }
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        currentUser = new User(rs.getString("name"), rs.getString("email"), rs.getString("password"),
                                rs.getString("role"));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            clearRememberedSession();
            return false;
        }

        clearRememberedSession();
        return false;
    }

    private static void migrateRememberedSessionIfNeeded() {
        int existingVersion = PREFS.getInt(KEY_SESSION_MIGRATION_VERSION, 0);
        if (existingVersion >= CURRENT_SESSION_MIGRATION_VERSION) {
            return;
        }

        // One-time cleanup for older remember-me data that could restore the wrong account.
        clearRememberedSession();
        PREFS.putInt(KEY_SESSION_MIGRATION_VERSION, CURRENT_SESSION_MIGRATION_VERSION);
    }

    public static ObservableList<House> getHouses() {
        ObservableList<House> list = FXCollections.observableArrayList();
        String query = "SELECT h.location, h.type, h.rent, h.image, h.bedrooms, h.bathrooms, h.area, u.name " +
                "FROM houses h JOIN users u ON h.owner_id = u.id";

        try (Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("DEBUG: Executing getHouses query: " + query);
            int rowCount = 0;
            while (rs.next()) {
                list.add(new House(
                        rs.getString("location"),
                        rs.getString("type"),
                        rs.getDouble("rent"),
                        rs.getString("name"),
                        rs.getString("image"),
                        rs.getInt("bedrooms"),
                        rs.getInt("bathrooms"),
                        rs.getDouble("area")));
                rowCount++;
            }
            System.out.println("DEBUG: getHouses fetched rows=" + rowCount);
        } catch (SQLException e) {
            System.out.println("ERROR: getHouses SQLException");
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

        String query = "SELECT r.id, u.name AS tenant_name, h.location, r.request_date, r.move_in_date, h.rent, r.status "
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
                    rs.getInt("id"),
                        rs.getString("tenant_name"),
                        rs.getString("location"),
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

    private static String safeValue(String value) {
        return value == null ? "" : value;
    }

    // --- KEEP YOUR EXISTING AUTH METHODS BELOW (validateUser, registerUser, etc)
    // ---
    public static boolean validateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE name = ? COLLATE NOCASE AND password = ?";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
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
        return registerUser(name, email, password, role, null);
    }

    public static boolean registerUser(String name, String email, String password, String role, String birthdate) {
        String query = "INSERT INTO users (name, email, password, role, phone, birthdate) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            String trimmedName = name == null ? "" : name.trim();
            String trimmedEmail = email == null ? "" : email.trim();
            pstmt.setString(1, trimmedName);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            if (!trimmedEmail.isBlank() && trimmedEmail.contains("@")) {
                pstmt.setString(2, trimmedEmail.toLowerCase());
                pstmt.setString(5, null);
                pstmt.setString(6, birthdate);
            } else if (!trimmedEmail.isBlank()) {
                pstmt.setString(2, null);
                pstmt.setString(5, normalizePhone(trimmedEmail));
                pstmt.setString(6, birthdate);
            } else {
                pstmt.setString(2, null);
                pstmt.setString(5, null);
                pstmt.setString(6, birthdate);
            }
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean emailExists(String email) {
        String query = "SELECT 1 FROM users WHERE lower(email) = lower(?)";
        try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email == null ? null : email.trim());
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean usernameExists(String username) {
        String query = "SELECT name FROM users WHERE name = ? COLLATE NOCASE";
        try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username == null ? null : username.trim());
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean phoneExists(String phone) {
        return findUserIdByPhone(phone) != null;
    }

    public static boolean contactExists(String contact) {
        String trimmedContact = contact == null ? "" : contact.trim();
        if (trimmedContact.isBlank()) {
            return false;
        }
        return findUserIdByContact(trimmedContact) != null;
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

    public static boolean updatePasswordByContact(String contact, String newPass) {
        String trimmedContact = contact == null ? "" : contact.trim();
        if (trimmedContact.isBlank()) {
            return false;
        }

        Integer userId = findUserIdByContact(trimmedContact);
        if (userId == null) {
            return false;
        }

        String query = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newPass);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Integer findUserIdByContact(String contact) {
        String trimmedContact = contact == null ? "" : contact.trim();
        if (trimmedContact.isBlank()) {
            return null;
        }
        // Try both paths so users can reset via either email or phone without strict input assumptions.
        if (trimmedContact.contains("@")) {
            Integer userId = findUserIdByEmail(trimmedContact);
            return userId != null ? userId : findUserIdByPhone(trimmedContact);
        }
        Integer userId = findUserIdByPhone(trimmedContact);
        return userId != null ? userId : findUserIdByEmail(trimmedContact);
    }

    private static Integer findUserIdByEmail(String email) {
        String query = "SELECT id FROM users WHERE lower(email) = lower(?) LIMIT 1";
        try (Connection conn = DatabaseConnection.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email == null ? null : email.trim());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            return null;
        }
        return null;
    }

    private static Integer findUserIdByPhone(String phone) {
        String normalizedTarget = normalizePhone(phone);
        if (normalizedTarget.isBlank()) {
            return null;
        }

        String query = "SELECT id, phone FROM users WHERE phone IS NOT NULL";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String storedPhone = rs.getString("phone");
                if (normalizedTarget.equals(normalizePhone(storedPhone))) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return null;
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String digitsOnly = phone.replaceAll("\\D", "");
        if (digitsOnly.startsWith("880") && digitsOnly.length() >= 13) {
            return digitsOnly.substring(2);
        }
        if (digitsOnly.startsWith("88") && digitsOnly.length() == 13) {
            return digitsOnly.substring(2);
        }
        return digitsOnly;
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