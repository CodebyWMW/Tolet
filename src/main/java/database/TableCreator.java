package database;

import java.sql.Connection;
import java.sql.Statement;

public class TableCreator {

    public static void createTables() {

        String userTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT CHECK(role IN ('tenant','owner')) NOT NULL,
                phone TEXT
            );
        """;

        String houseTable = """
            CREATE TABLE IF NOT EXISTS houses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                owner_id INTEGER NOT NULL,
                address TEXT NOT NULL,
                city TEXT NOT NULL,
                rent REAL NOT NULL,
                bedrooms INTEGER,
                type TEXT,
                description TEXT,
                status TEXT DEFAULT 'AVAILABLE'
                       CHECK(status IN ('AVAILABLE','UNAVAILABLE')),
                FOREIGN KEY (owner_id)
                    REFERENCES users(id)
                    ON DELETE CASCADE
            );
        """;

        String requestTable = """
            CREATE TABLE IF NOT EXISTS rent_requests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                house_id INTEGER NOT NULL,
                renter_id INTEGER NOT NULL,
                status TEXT DEFAULT 'PENDING'
                       CHECK(status IN ('PENDING','APPROVED','REJECTED')),
                FOREIGN KEY (house_id)
                    REFERENCES houses(id)
                    ON DELETE CASCADE,
                FOREIGN KEY (renter_id)
                    REFERENCES users(id)
                    ON DELETE CASCADE
            );
        """;

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {

            // VERY IMPORTANT for SQLite
            stmt.execute("PRAGMA foreign_keys = ON");

            stmt.execute(userTable);
            stmt.execute(houseTable);
            stmt.execute(requestTable);

            System.out.println("✅ Tables created successfully!");

        } catch (Exception e) {
            System.out.println("❌ Failed to create tables");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        createTables();
    }
}