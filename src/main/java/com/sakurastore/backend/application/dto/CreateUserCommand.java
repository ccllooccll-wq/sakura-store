package com.sakurastore.backend.application.dto;

public class CreateUserCommand {
    private String username;
    private String fullName;
    private String email;
    private String password;
    private Long roleId;

    public CreateUserCommand() {
    }

    public CreateUserCommand(String username, String fullName, String email, String password, Long roleId) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.roleId = roleId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
