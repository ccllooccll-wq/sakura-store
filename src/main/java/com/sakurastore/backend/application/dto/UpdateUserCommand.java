package com.sakurastore.backend.application.dto;

public class UpdateUserCommand {
    private String fullName;
    private String email;
    private Long roleId;

    public UpdateUserCommand() {
    }

    public UpdateUserCommand(String fullName, String email, Long roleId) {
        this.fullName = fullName;
        this.email = email;
        this.roleId = roleId;
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

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
