package services;

import java.io.IOException;

import models.User;
import network.ClientConnection;

public class UserService {

    private String lastErrorMessage;

    public boolean registerUser(User user) {

        lastErrorMessage = null;

        String username = user.getName() == null ? "" : user.getName().trim();
        String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        String phone = user.getPhone() == null ? "" : user.getPhone().trim();

        if (username.isBlank()) {
            lastErrorMessage = "Username is required.";
            return false;
        }

        // Either email OR phone is required, not both
        if (email.isBlank() && phone.isBlank()) {
            lastErrorMessage = "Email or phone number is required.";
            return false;
        }

        String role = user.getRole() == null ? "" : user.getRole().trim();
        String password = user.getPassword() == null ? "" : user.getPassword().trim();
        String birthdate = user.getBirthdate() == null ? "" : user.getBirthdate().trim();

        // Validate password
        if (password.isBlank()) {
            lastErrorMessage = "Password is required.";
            return false;
        }

        // Validate role
        if (role.isBlank()) {
            lastErrorMessage = "Role is required.";
            return false;
        }

        String command = "SIGNUP|"
                + sanitize(username) + "|"
                + sanitize(email) + "|"
                + sanitize(password) + "|"
                + sanitize(role) + "|"
                + sanitize(phone) + "|"
                + sanitize(birthdate);

        System.out.println("[UserService] Sending SIGNUP command: SIGNUP|" + username + "|" + email + "|***|" + role + "|" + phone + "|" + birthdate);
        System.out.println("[UserService] Raw command parts: " + command.split("\\|").length);

        try {
            System.out.println("[UserService] Sending to server...");
            String response = ClientConnection.sendCommand(command);
            System.out.println("[UserService] Server response: " + response);
            
            if ("SUCCESS".equals(response)) {
                System.out.println("[UserService] Registration SUCCESS!");
                return true;
            }

            if (response != null && response.startsWith("ERROR:")) {
                lastErrorMessage = response.substring("ERROR:".length()).replace('_', ' ');
                System.out.println("[UserService] Server error: " + lastErrorMessage);
            } else {
                lastErrorMessage = "Registration failed.";
                System.out.println("[UserService] Unexpected response: " + response);
            }
            return false;
        } catch (IOException e) {
            lastErrorMessage = "Could not connect to server.";
            System.err.println("[UserService] Connection error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", " ").trim();
    }
}