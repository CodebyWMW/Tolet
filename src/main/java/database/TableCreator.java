package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TableCreator {
    public static void createTables() {
        String userTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT UNIQUE COLLATE NOCASE,"
                                + "email TEXT UNIQUE COLLATE NOCASE,"
                                + "phone TEXT UNIQUE,"
                                + "password TEXT,"
                                + "role TEXT,"
                                + "birthdate TEXT,"
                                + "verified INTEGER DEFAULT 0,"
                                + "public_id TEXT UNIQUE)";

        // Updated houses table with new columns matching DataStore expectations
        String houseTable = "CREATE TABLE IF NOT EXISTS houses (" 
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "location TEXT,"
                + "owner_id INTEGER,"
                + "type TEXT,"
                + "image TEXT,"
                + "bedrooms INTEGER DEFAULT 0,"
                + "bathrooms INTEGER DEFAULT 0,"
                + "area REAL DEFAULT 0.0,"
                + "family_allowed INTEGER DEFAULT 0,"
                + "bachelor_allowed INTEGER DEFAULT 0,"
                + "gas_available INTEGER DEFAULT 0,"
                + "parking_available INTEGER DEFAULT 0,"
                + "furnished INTEGER DEFAULT 0,"
                + "pet_friendly INTEGER DEFAULT 0,"
                + "title TEXT,"
                + "rent REAL,"
                + "approval_status TEXT DEFAULT 'pending',"
                + "FOREIGN KEY(owner_id) REFERENCES users(id))";

        String requestTable = "CREATE TABLE IF NOT EXISTS rent_requests ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "house_id INTEGER,"
                + "tenant_id INTEGER,"
                + "request_date TEXT,"
                + "move_in_date TEXT,"
                + "accepted_at TEXT,"
                + "status TEXT,"
                + "FOREIGN KEY(house_id) REFERENCES houses(id),"
                + "FOREIGN KEY(tenant_id) REFERENCES users(id))";

        String reviewsTable = "CREATE TABLE IF NOT EXISTS house_reviews ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "house_id INTEGER NOT NULL,"
                + "tenant_id INTEGER NOT NULL,"
                + "review_text TEXT NOT NULL,"
                + "status TEXT DEFAULT 'submitted',"
                + "created_at TEXT DEFAULT CURRENT_DATE,"
                + "updated_at TEXT DEFAULT CURRENT_DATE,"
                + "FOREIGN KEY(house_id) REFERENCES houses(id) ON DELETE CASCADE,"
                + "FOREIGN KEY(tenant_id) REFERENCES users(id) ON DELETE CASCADE,"
                + "UNIQUE(house_id, tenant_id))";

        String userAuditTable = "CREATE TABLE IF NOT EXISTS users_audit ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER,"
                + "public_id TEXT,"
                + "name TEXT,"
                + "email TEXT,"
                + "phone TEXT,"
                + "role TEXT,"
                + "deleted_at TEXT,"
                + "deleted_by TEXT)";

                String houseImagesTable = "CREATE TABLE IF NOT EXISTS house_images ("
                                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "house_id INTEGER NOT NULL,"
                                + "image_name TEXT,"
                                + "image_data BLOB NOT NULL,"
                                + "sort_order INTEGER DEFAULT 1,"
                                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
                                + "FOREIGN KEY(house_id) REFERENCES houses(id) ON DELETE CASCADE)";

        try (Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement()) {
            stmt.execute(userTable);
                        ensureUserSchema(conn, stmt);
            stmt.execute(houseTable);
                        ensureHouseSchema(stmt);
            stmt.execute(requestTable);
                                                ensureRentRequestSchema(stmt);
            stmt.execute(userAuditTable);
                        ensureUserAuditSchema(stmt);
                        stmt.execute(houseImagesTable);
                                                stmt.execute(reviewsTable);
                                                stmt.execute("CREATE INDEX IF NOT EXISTS idx_house_reviews_house_id ON house_reviews(house_id)");
                                                stmt.execute("CREATE INDEX IF NOT EXISTS idx_house_reviews_tenant_id ON house_reviews(tenant_id)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_house_images_house_id ON house_images(house_id)");
                        cleanupOrphanOwnerListings(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

        private static void cleanupOrphanOwnerListings(Connection conn) throws SQLException {
                String deleteRequestsSql = "DELETE FROM rent_requests "
                                + "WHERE house_id IN (SELECT h.id FROM houses h "
                                + "LEFT JOIN users u ON u.id = h.owner_id WHERE u.id IS NULL)";
                String deleteImagesSql = "DELETE FROM house_images "
                                + "WHERE house_id IN (SELECT h.id FROM houses h "
                                + "LEFT JOIN users u ON u.id = h.owner_id WHERE u.id IS NULL)";
                String deleteHousesSql = "DELETE FROM houses "
                                + "WHERE id IN (SELECT h.id FROM houses h "
                                + "LEFT JOIN users u ON u.id = h.owner_id WHERE u.id IS NULL)";

                try (PreparedStatement deleteRequestsStmt = conn.prepareStatement(deleteRequestsSql);
                                PreparedStatement deleteImagesStmt = conn.prepareStatement(deleteImagesSql);
                                PreparedStatement deleteHousesStmt = conn.prepareStatement(deleteHousesSql)) {
                        deleteRequestsStmt.executeUpdate();
                        deleteImagesStmt.executeUpdate();
                        deleteHousesStmt.executeUpdate();
                }
        }

        private static void ensureUserSchema(Connection conn, Statement stmt) throws SQLException {
                Set<String> columns = getColumns(stmt, "users");

                addColumnIfMissing(stmt, columns, "users", "email", "email TEXT");
                addColumnIfMissing(stmt, columns, "users", "phone", "phone TEXT");
                addColumnIfMissing(stmt, columns, "users", "birthdate", "birthdate TEXT");
                addColumnIfMissing(stmt, columns, "users", "verified", "verified INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "users", "public_id", "public_id TEXT");

                backfillPublicUserIds(conn);
                normalizeLegacyPublicUserPrefixes(conn);

                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_name_unique ON users(name COLLATE NOCASE)");
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_unique ON users(email COLLATE NOCASE)");
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone_unique ON users(phone)");
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_public_id_unique ON users(public_id)");
        }

        private static void ensureUserAuditSchema(Statement stmt) throws SQLException {
                Set<String> columns = getColumns(stmt, "users_audit");
                addColumnIfMissing(stmt, columns, "users_audit", "public_id", "public_id TEXT");
        }

        private static void ensureHouseSchema(Statement stmt) throws SQLException {
                Set<String> columns = getColumns(stmt, "houses");

                addColumnIfMissing(stmt, columns, "houses", "location", "location TEXT");
                addColumnIfMissing(stmt, columns, "houses", "owner_id", "owner_id INTEGER");
                addColumnIfMissing(stmt, columns, "houses", "type", "type TEXT");
                addColumnIfMissing(stmt, columns, "houses", "image", "image TEXT");
                addColumnIfMissing(stmt, columns, "houses", "bedrooms", "bedrooms INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "bathrooms", "bathrooms INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "area", "area REAL DEFAULT 0.0");
                addColumnIfMissing(stmt, columns, "houses", "family_allowed", "family_allowed INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "bachelor_allowed", "bachelor_allowed INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "gas_available", "gas_available INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "parking_available", "parking_available INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "furnished", "furnished INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "pet_friendly", "pet_friendly INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "title", "title TEXT");
                addColumnIfMissing(stmt, columns, "houses", "rent", "rent REAL");
                addColumnIfMissing(stmt, columns, "houses", "approval_status", "approval_status TEXT DEFAULT 'pending'");

                // Fields for modern owner listing form.
                addColumnIfMissing(stmt, columns, "houses", "short_detail", "short_detail TEXT");
                addColumnIfMissing(stmt, columns, "houses", "details", "details TEXT");
                addColumnIfMissing(stmt, columns, "houses", "tags", "tags TEXT");
                addColumnIfMissing(stmt, columns, "houses", "availability", "availability TEXT");
                addColumnIfMissing(stmt, columns, "houses", "water_available", "water_available INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "current_available", "current_available INTEGER DEFAULT 0");
                addColumnIfMissing(stmt, columns, "houses", "contact_info", "contact_info TEXT");
        }

        private static void ensureRentRequestSchema(Statement stmt) throws SQLException {
                Set<String> columns = getColumns(stmt, "rent_requests");
                addColumnIfMissing(stmt, columns, "rent_requests", "accepted_at", "accepted_at TEXT");

                // Backfill accepted_at for legacy approved rows where timestamp is missing.
                stmt.execute("UPDATE rent_requests SET accepted_at = COALESCE(NULLIF(TRIM(request_date), ''), '"
                                + LocalDate.now() + "') "
                                + "WHERE lower(trim(COALESCE(status, ''))) = 'approved' "
                                + "AND (accepted_at IS NULL OR TRIM(accepted_at) = '')");
        }

        private static Set<String> getColumns(Statement stmt, String tableName) throws SQLException {
                Set<String> columns = new HashSet<>();
                try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
                        while (rs.next()) {
                                columns.add(rs.getString("name").toLowerCase());
                        }
                }
                return columns;
        }

        private static void addColumnIfMissing(Statement stmt, Set<String> columns, String tableName,
                        String columnName, String definition) throws SQLException {
                if (!columns.contains(columnName.toLowerCase())) {
                        stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + definition);
                        columns.add(columnName.toLowerCase());
                }
        }

        private static void backfillPublicUserIds(Connection conn) throws SQLException {
                String readSql = "SELECT id, role FROM users WHERE public_id IS NULL OR TRIM(public_id) = '' ORDER BY id ASC";
                try (PreparedStatement readStmt = conn.prepareStatement(readSql);
                                ResultSet rs = readStmt.executeQuery()) {
                        while (rs.next()) {
                                int userId = rs.getInt("id");
                                String role = rs.getString("role");
                                String nextPublicId = generateNextPublicId(conn, role);

                                try (PreparedStatement updateStmt = conn
                                                .prepareStatement("UPDATE users SET public_id = ? WHERE id = ?")) {
                                        updateStmt.setString(1, nextPublicId);
                                        updateStmt.setInt(2, userId);
                                        updateStmt.executeUpdate();
                                }
                        }
                }
        }

        private static String generateNextPublicId(Connection conn, String role) throws SQLException {
                String prefix = mapRolePrefix(role);

                int maxSequence = 0;
                String sql = "SELECT public_id FROM users WHERE public_id LIKE ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, prefix + "%");
                        try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                        String existing = rs.getString("public_id");
                                        if (existing == null || !existing.startsWith(prefix)
                                                        || existing.length() <= prefix.length()) {
                                                continue;
                                        }
                                        String suffix = existing.substring(prefix.length());
                                        try {
                                                int seq = Integer.parseInt(suffix);
                                                if (seq > maxSequence) {
                                                        maxSequence = seq;
                                                }
                                        } catch (NumberFormatException ignored) {
                                                // Skip malformed IDs and continue.
                                        }
                                }
                        }
                }

                return prefix + String.format("%03d", maxSequence + 1);
        }

        private static void normalizeLegacyPublicUserPrefixes(Connection conn) throws SQLException {
                String sql = "SELECT id, role, public_id FROM users WHERE public_id LIKE 'user%' "
                                + "AND lower(trim(COALESCE(role, ''))) IN ('owner', 'house owner', 'bariwala', 'landlord', 'tenant', 'varatia') "
                                + "ORDER BY id ASC";
                List<Integer> userIdsToNormalize = new ArrayList<>();
                List<String> rolesToNormalize = new ArrayList<>();

                try (PreparedStatement ps = conn.prepareStatement(sql);
                                ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                                String role = rs.getString("role");
                                userIdsToNormalize.add(rs.getInt("id"));
                                rolesToNormalize.add(role);
                        }
                }

                for (int i = 0; i < userIdsToNormalize.size(); i++) {
                        int userId = userIdsToNormalize.get(i);
                        String role = rolesToNormalize.get(i);
                        String nextPublicId = generateNextPublicId(conn, role);

                        try (PreparedStatement updateStmt = conn
                                        .prepareStatement("UPDATE users SET public_id = ? WHERE id = ?")) {
                                updateStmt.setString(1, nextPublicId);
                                updateStmt.setInt(2, userId);
                                updateStmt.executeUpdate();
                        }
                }
        }

        private static String mapRolePrefix(String role) {
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