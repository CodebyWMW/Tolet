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
    public void registerUser(User user) {
        String sql = "INSERT INTO users (name, email, password, role, phone) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getPhone());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // LOGIN USER
    public User loginUser(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            // Check if a user is found
            if (rs.next()) {
                User user = new User();

                // Use setters to set private variables
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setPhone(rs.getString("phone"));

                return user; // login success
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // login failed
    }

    // FIND USER BY ID (optional, useful later)
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

                // Handle verified field (may not exist in old schema)
                try {
                    user.setVerified(rs.getInt("verified") == 1);
                } catch (SQLException e) {
                    user.setVerified(false);
                }

                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // FIND USER BY EMAIL
    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
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
                } catch (SQLException e) {
                    user.setVerified(false);
                }
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ADMIN: Get all users
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
                } catch (SQLException e) {
                    user.setVerified(false);
                }
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    // ADMIN: Verify/Unverify user
    public boolean updateUserVerification(int userId, boolean verified) {
        String sql = "UPDATE users SET verified = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, verified ? 1 : 0);
            pstmt.setInt(2, userId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ADMIN: Delete user permanently
    public boolean deleteUserById(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ADMIN: Delete user permanently with audit record
    public boolean deleteUserWithAudit(int userId, String deletedBy) {
        String insertAuditSql = "INSERT INTO users_audit (user_id, name, email, phone, role, deleted_at, deleted_by) "
                + "SELECT id, name, email, phone, role, datetime('now'), ? FROM users WHERE id = ?";
        String deleteSql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return false;
            }
            conn.setAutoCommit(false);

            try (PreparedStatement auditStmt = conn.prepareStatement(insertAuditSql);
                    PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

                auditStmt.setString(1, deletedBy);
                auditStmt.setInt(2, userId);
                int inserted = auditStmt.executeUpdate();

                deleteStmt.setInt(1, userId);
                int deleted = deleteStmt.executeUpdate();

                if (inserted > 0 && deleted > 0) {
                    conn.commit();
                    return true;
                }

                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ADMIN: Get users by role
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
                } catch (SQLException e) {
                    user.setVerified(false);
                }
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    // ADMIN: Get deleted users audit log
    public List<models.UserAudit> getUserAuditLog() {
        List<models.UserAudit> audit = new ArrayList<>();
        String sql = "SELECT user_id, name, email, phone, role, deleted_at, deleted_by "
                + "FROM users_audit ORDER BY deleted_at DESC";

        try (Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                audit.add(new models.UserAudit(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getString("deleted_at"),
                        rs.getString("deleted_by")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return audit;
    }
}