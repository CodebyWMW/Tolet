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
import models.UserAudit;
public class UserDAO {

    // ================= REGISTER USER =================
    public boolean registerUser(User user) {

        String sql = "INSERT INTO users (name, email, password, role, phone, birthdate, public_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String role = user.getRole() == null ? "" : user.getRole().trim().toLowerCase();
            String publicId = generateNextPublicId(conn, role);

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, role);
            pstmt.setString(5, user.getPhone());
            pstmt.setString(6, user.getBirthdate());
            pstmt.setString(7, publicId);

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
                user.setPublicId(rs.getString("public_id"));
                user.setPhone(rs.getString("phone"));

                try {
                    user.setBirthdate(rs.getString("birthdate"));
                } catch (SQLException ignored) {
                    user.setBirthdate(null);
                }

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
                user.setPublicId(rs.getString("public_id"));
                user.setPhone(rs.getString("phone"));

                try {
                    user.setBirthdate(rs.getString("birthdate"));
                } catch (SQLException ignored) {
                    user.setBirthdate(null);
                }

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

        String selectSql = "SELECT id, public_id, name, email, phone, role FROM users WHERE id = ?";
        String auditSql = "INSERT INTO users_audit (user_id, public_id, name, email, phone, role, deleted_at, deleted_by) VALUES (?, ?, ?, ?, ?, ?, datetime('now'), ?)";
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
                    String publicId = rs.getString("public_id");

                    try (PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                         PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

                        auditStmt.setInt(1, userId);
                        auditStmt.setString(2, publicId);
                        auditStmt.setString(3, name);
                        auditStmt.setString(4, email);
                        auditStmt.setString(5, phone);
                        auditStmt.setString(6, role);
                        auditStmt.setString(7, deletedBy);
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

    public List<UserAudit> getAuditLog() {

        List<UserAudit> auditLog = new ArrayList<>();
        String sql = "SELECT user_id, public_id, name, email, phone, role, deleted_at, deleted_by FROM users_audit ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UserAudit audit = new UserAudit(
                        rs.getInt("user_id"),
                    rs.getString("public_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getString("deleted_at"),
                        rs.getString("deleted_by"));
                auditLog.add(audit);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return auditLog;
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
                user.setPublicId(rs.getString("public_id"));
                user.setPhone(rs.getString("phone"));

                try {
                    user.setBirthdate(rs.getString("birthdate"));
                } catch (SQLException ignored) {
                    user.setBirthdate(null);
                }

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
                user.setPublicId(rs.getString("public_id"));
                user.setPhone(rs.getString("phone"));

                try {
                    user.setBirthdate(rs.getString("birthdate"));
                } catch (SQLException ignored) {
                    user.setBirthdate(null);
                }

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

    public int normalizeExistingOwnerAndTenantPublicIds() {
        String selectSql = "SELECT id, role FROM users WHERE "
            + "(public_id IS NULL OR trim(public_id) = '' OR lower(public_id) LIKE 'user%') "
            + "AND lower(trim(COALESCE(role, ''))) IN ('owner', 'house owner', 'bariwala', 'landlord', 'tenant', 'varatia') "
            + "ORDER BY id ASC";
        String updateSql = "UPDATE users SET public_id = ? WHERE id = ?";

        int updatedCount = 0;

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                ResultSet rs = selectStmt.executeQuery()) {

            while (rs.next()) {
                int userId = rs.getInt("id");
                String role = rs.getString("role");
                String nextPublicId = generateNextPublicId(conn, role);

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, nextPublicId);
                    updateStmt.setInt(2, userId);
                    updatedCount += updateStmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return updatedCount;
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
            return "murgi";
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
        return "murgi";
    }
}
