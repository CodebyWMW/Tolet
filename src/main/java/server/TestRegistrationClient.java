package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Test client for registration debugging
 * Tests SIGNUP with various scenarios
 */
public class TestRegistrationClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void main(String[] args) {
        try {
            System.out.println("========================================");
            System.out.println("REGISTRATION TEST CLIENT");
            System.out.println("========================================");
            System.out.println();

            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("[" + timestamp() + "] Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
            System.out.println();

            // Test Case 1: Valid registration with unique data
            System.out.println("TEST 1: Valid Registration");
            System.out.println("----------------------------------");
            String uniqueName = "testuser" + System.currentTimeMillis();
            String uniqueEmail = "test" + System.currentTimeMillis() + "@example.com";
            String uniquePhone = "980" + (1000000 + (Math.random() * 8999999));
            
            testSignup(out, in, uniqueName, uniqueEmail, "password123", "tenant", uniquePhone, null);
            waitBetweenTests(1000);

            // Test Case 2: Missing Password (should be caught by new validation)
            System.out.println("\nTEST 2: Missing Password");
            System.out.println("----------------------------------");
            testSignup(out, in, "anotheruser", "another@example.com", "", "tenant", "9800012345", null);
            waitBetweenTests(1000);

            // Test Case 3: Missing Email  
            System.out.println("\nTEST 3: Missing Email");
            System.out.println("----------------------------------");
            testSignup(out, in, "someuser", "", "pass123", "owner", "9800034567", null);
            waitBetweenTests(1000);

            // Test Case 4: Duplicate Email (same email as Test 1)
            System.out.println("\nTEST 4: Duplicate Email");
            System.out.println("----------------------------------");
            testSignup(out, in, "different" + System.currentTimeMillis(), uniqueEmail, "password456", "tenant", "9800056789", null);
            waitBetweenTests(1000);

            // Test Case 5: Duplicate Phone (same phone as Test 1)
            System.out.println("\nTEST 5: Duplicate Phone");
            System.out.println("----------------------------------");
            testSignup(out, in, "yetanother" + System.currentTimeMillis(), "yetanother@example.com", "pass", "owner", uniquePhone.substring(0, 10), null);
            waitBetweenTests(1000);

            // Test Case 6: With Birthdate
            System.out.println("\nTEST 6: Valid Registration with Birthdate");
            System.out.println("----------------------------------");
            String uniqueName2 = "birthdateuser" + System.currentTimeMillis();
            testSignup(out, in, uniqueName2, "birthday" + System.currentTimeMillis() + "@example.com", "pass123!", "tenant", "9801112223", "1990-05-15");
            waitBetweenTests(1000);

            System.out.println();
            System.out.println("========================================");
            System.out.println("ALL TESTS COMPLETED");
            System.out.println("========================================");
            System.out.println();
            System.out.println("📝 Check server console for detailed logs starting with 'SIGNUP:' and 'Registration error:'");

            socket.close();

        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testSignup(PrintWriter out, BufferedReader in, 
                                   String name, String email, String password, 
                                   String role, String phone, String birthdate) {
        try {
            // Build command
            String command;
            if (birthdate != null && !birthdate.isEmpty()) {
                command = "SIGNUP|" + name + "|" + email + "|" + password + "|" + role + "|" + phone + "|" + birthdate;
            } else {
                command = "SIGNUP|" + name + "|" + email + "|" + password + "|" + role + "|" + phone;
            }

            System.out.println("Sending: SIGNUP|" + name + "|" + email + "|***|" + role + "|" + phone + 
                             (birthdate != null ? "|" + birthdate : ""));
            System.out.println("[" + timestamp() + "] Sending request...");

            out.println(command);
            String response = in.readLine();

            System.out.println("[" + timestamp() + "] Response: " + response);

            if ("SUCCESS".equals(response)) {
                System.out.println("✅ PASSED - Registration successful!");
            } else if (response.startsWith("ERROR:")) {
                String errorMsg = response.substring("ERROR:".length()).replace('_', ' ');
                System.out.println("❌ FAILED - Error: " + errorMsg);
            } else {
                System.out.println("⚠️ WARNING - Unexpected response: " + response);
            }

        } catch (Exception e) {
            System.err.println("❌ EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void waitBetweenTests(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String timestamp() {
        return LocalDateTime.now().format(formatter);
    }
}
