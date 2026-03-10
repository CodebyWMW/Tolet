package com.tolet;

public class User {
    private String username;
    private String email; // Added this
    private String password;
    private String role;
    private int id;
    public User(String username, String email, String password, String role,int id) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.id=id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }
    public int getId(){
        return id;
    }
}