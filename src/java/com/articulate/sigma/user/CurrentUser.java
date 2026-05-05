package com.articulate.sigma.user;

public class CurrentUser {

    private final String username;
    private final String role;

    public CurrentUser(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
}