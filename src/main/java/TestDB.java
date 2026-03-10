import java.sql.Connection;
import java.sql.SQLException;

import database.DatabaseConnection;

public class TestDB {

    public static void main(String[] args) throws SQLException {

        Connection conn = DatabaseConnection.connect();

        if (conn != null) {
            System.out.println("Connection test PASSED ✅");
        } else {
            System.out.println("Connection test FAILED ❌");
        }
    }
}