package database;

import java.sql.Connection;

public class TestDB {

    public static void main(String[] args) {

        Connection conn = DatabaseConnection.connect();

        if (conn != null) {
            System.out.println("🎉 Connection test PASSED");
        } else {
            System.out.println("❌ Connection test FAILED");
        }
    }
}
