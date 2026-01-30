package com.lektralabs.thrones.pallbearer.api.model.partial;

public class LoginUser {
    private final String username;
    private final String password;

    public LoginUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
