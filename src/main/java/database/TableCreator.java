package database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
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
                                + "verified INTEGER DEFAULT 0)";

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
                + "status TEXT,"
                + "FOREIGN KEY(house_id) REFERENCES houses(id),"
                + "FOREIGN KEY(tenant_id) REFERENCES users(id))";

        String userAuditTable = "CREATE TABLE IF NOT EXISTS users_audit ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER,"
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
                        ensureUserSchema(stmt);
            stmt.execute(houseTable);
                        ensureHouseSchema(stmt);
            stmt.execute(requestTable);
            stmt.execute(userAuditTable);
                        stmt.execute(houseImagesTable);
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_house_images_house_id ON house_images(house_id)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

        private static void ensureUserSchema(Statement stmt) throws SQLException {
                Set<String> columns = getColumns(stmt, "users");

                addColumnIfMissing(stmt, columns, "users", "email", "email TEXT");
                addColumnIfMissing(stmt, columns, "users", "phone", "phone TEXT");
                addColumnIfMissing(stmt, columns, "users", "birthdate", "birthdate TEXT");
                addColumnIfMissing(stmt, columns, "users", "verified", "verified INTEGER DEFAULT 0");

                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_name_unique ON users(name COLLATE NOCASE)");
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_unique ON users(email COLLATE NOCASE)");
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone_unique ON users(phone)");
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
}