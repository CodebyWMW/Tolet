package com.tolet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;

public class DataStore {
    // Shared lists accessible from anywhere in the app
    public static ObservableList<User> users = FXCollections.observableArrayList();
    public static ObservableList<House> houses = FXCollections.observableArrayList();
    public static User currentUser; // Tracks who is logged in
    public static boolean darkMode = false;

    public static void initData() {
        // Pre-loaded users for testing
        users.add(new User("admin", "123", "Admin"));
        users.add(new User("owner", "123", "House Owner"));
        users.add(new User("tenant", "123", "Tenant"));

        // Pre-loaded houses
        houses.add(new House("Dhaka, Dhanmondi", "Family", 25000, "owner"));
        houses.add(new House("Dhaka, Mirpur", "Bachelor (M)", 5000, "owner"));
    }

    public static boolean validate(String user, String pass, String role) {
        for (User u : users) {
            if (u.getUsername().equals(user) && u.getPassword().equals(pass) && u.getRole().equals(role)) {
                currentUser = u;
                return true;
            }
        }
        return false;
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        String darkThemeUrl = DataStore.class.getResource("dark-theme.css").toExternalForm();
        if (darkMode) {
            if (!scene.getStylesheets().contains(darkThemeUrl)) {
                scene.getStylesheets().add(darkThemeUrl);
            }
        } else {
            scene.getStylesheets().remove(darkThemeUrl);
        }
    }
}