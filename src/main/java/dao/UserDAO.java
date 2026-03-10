package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import database.DatabaseConnection;
import models.User;
public class UserDAO {

    // ================= REGISTER USER =================
    public boolean registerUser(User user) {

        String sql = "INSERT INTO users (name, email, password, role, phone) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getPhone());

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
  
    // ================= LOGIN USER =================
    public User loginUser(String email, String password) {

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setPhone(rs.getString("phone"));

                try {
                    user.setVerified(rs.getInt("verified") == 1);
                } catch (SQLException ignored) {
                    user.setVerified(false);
                }

                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

public boolean updateUserVerification(int userId, boolean verified) {
    String sql = "UPDATE users SET verified = ? WHERE id = ?";

    try (Connection conn = DatabaseConnection.connect();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setBoolean(1, verified); // if your DB column is INT, use: ps.setInt(1, verified ? 1 : 0);
        ps.setInt(2, userId);

        return ps.executeUpdate() > 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

    public List<User> getUsersByRole(String role) {

        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setPhone(rs.getString("phone"));

                try {
                    user.setVerified(rs.getInt("verified") == 1);
                } catch (SQLException ignored) {
                    user.setVerified(false);
                }

                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    public boolean deleteUserWithAudit(int userId, String deletedBy) {

        String selectSql = "SELECT id, name, email, phone, role FROM users WHERE id = ?";
        String auditSql = "INSERT INTO users_audit (user_id, name, email, phone, role, deleted_at, deleted_by) VALUES (?, ?, ?, ?, ?, datetime('now'), ?)";
        String deleteSql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect()) {

            conn.setAutoCommit(false);

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, userId);

                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    String phone = rs.getString("phone");
                    String role = rs.getString("role");

                    try (PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                         PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

                        auditStmt.setInt(1, userId);
                        auditStmt.setString(2, name);
                        auditStmt.setString(3, email);
                        auditStmt.setString(4, phone);
                        auditStmt.setString(5, role);
                        auditStmt.setString(6, deletedBy);
                        auditStmt.executeUpdate();

                        deleteStmt.setInt(1, userId);
                        int deletedRows = deleteStmt.executeUpdate();

                        if (deletedRows > 0) {
                            conn.commit();
                            return true;
                        }
                    }
                }
            }

            conn.rollback();
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================= FIND USER BY ID =================
    public User getUserById(int id) {

        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setPhone(rs.getString("phone"));

                try {
                    user.setVerified(rs.getInt("verified") == 1);
                } catch (SQLException ignored) {
                    user.setVerified(false);
                }

                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ================= GET ALL USERS =================
    public List<User> getAllUsers() {

        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {

                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setPhone(rs.getString("phone"));

                try {
                    user.setVerified(rs.getInt("verified") == 1);
                } catch (SQLException ignored) {
                    user.setVerified(false);
                }

                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    // ================= DELETE USER =================
    public boolean deleteUserById(int userId) {

        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}