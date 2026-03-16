package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import database.DatabaseConnection;
import models.User;

public class UserService {

    private String lastErrorMessage;

    public boolean registerUser(User user) {

        lastErrorMessage = null;

        String username = user.getName() == null ? "" : user.getName().trim();
        String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        String phone = user.getPhone() == null ? "" : user.getPhone().trim();

        if (username.isBlank()) {
            lastErrorMessage = "Username is required.";
            return false;
        }

        if (email.isBlank()) {
            lastErrorMessage = "Email is required.";
            return false;
        }

        if (phone.isBlank()) {
            lastErrorMessage = "Phone is required.";
            return false;
        }

        if (valueExists("SELECT 1 FROM users WHERE name = ? COLLATE NOCASE", username)) {
            lastErrorMessage = "Username already exists.";
            return false;
        }

        if (valueExists("SELECT 1 FROM users WHERE lower(email) = lower(?)", email)) {
            lastErrorMessage = "Email already exists.";
            return false;
        }

        if (valueExists("SELECT 1 FROM users WHERE phone = ?", phone)) {
            lastErrorMessage = "Phone already exists.";
            return false;
        }

        String sql = "INSERT INTO users (name, email, password, role, phone, birthdate, public_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String role = user.getRole() == null ? "" : user.getRole().trim().toLowerCase();
            String publicId = generateNextPublicId(conn, role);

            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, user.getPassword()); // later you can hash it
            ps.setString(4, role);
            ps.setString(5, phone);
            ps.setString(6, user.getBirthdate());
            ps.setString(7, publicId);

            int rowsInserted = ps.executeUpdate();

            return rowsInserted > 0;

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) {
                lastErrorMessage = "Username, email, or phone already exists.";
            } else {
                lastErrorMessage = "Registration failed.";
            }
            return false;
        }
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private boolean valueExists(String sql, String value) {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private String generateNextPublicId(Connection conn, String role) throws SQLException {
        String prefix = mapRolePrefix(role);

        String likePattern = prefix + "%";
        String sql = "SELECT public_id FROM users WHERE public_id LIKE ?";
        int maxSequence = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, likePattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String existing = rs.getString("public_id");
                    if (existing == null || !existing.startsWith(prefix) || existing.length() <= prefix.length()) {
                        continue;
                    }
                    String suffix = existing.substring(prefix.length());
                    try {
                        int seq = Integer.parseInt(suffix);
                        if (seq > maxSequence) {
                            maxSequence = seq;
                        }
                    } catch (NumberFormatException ignored) {
                        // Ignore malformed historical values and keep scanning.
                    }
                }
            }
        }

        return prefix + String.format("%03d", maxSequence + 1);
    }

    private String mapRolePrefix(String role) {
        if (role == null) {
            return "user";
        }

        String normalized = role.trim().toLowerCase();
        if (normalized.equals("tenant") || normalized.equals("varatia")) {
            return "Varatia";
        }
        if (normalized.equals("owner")
                || normalized.equals("house owner")
                || normalized.equals("bariwala")
                || normalized.equals("landlord")) {
            return "Bariwala";
        }
        return "user";
    }
}