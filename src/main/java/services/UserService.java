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

        String sql = "INSERT INTO users (name, email, password, role, phone, birthdate) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, user.getPassword()); // later you can hash it
            ps.setString(4, user.getRole());
            ps.setString(5, phone);
            ps.setString(6, user.getBirthdate());

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
}