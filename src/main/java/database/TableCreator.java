package database;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class TableCreator {
    public static void createTables() {
        String userTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT UNIQUE COLLATE NOCASE,"
                + "email TEXT UNIQUE,"
                + "phone TEXT,"
                + "password TEXT,"
                + "role TEXT,"
                + "verified INTEGER DEFAULT 0)";

        // Updated houses table with new columns matching House model
        String houseTable = "CREATE TABLE IF NOT EXISTS houses ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "location TEXT,"
                + "owner_id INTEGER,"
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

        try (Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement()) {
            stmt.execute(userTable);
            stmt.execute(houseTable);
            stmt.execute(requestTable);
            stmt.execute(userAuditTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}