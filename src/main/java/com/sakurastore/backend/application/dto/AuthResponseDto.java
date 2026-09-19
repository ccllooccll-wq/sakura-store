package com.sakurastore.backend.application.dto;

public class AuthResponseDto {
    private String token;
    private String tokenType = "Bearer";
    private UserResponseDto user;
    private boolean requiresOtp;
    private String email;
    private String mensaje;

    public AuthResponseDto() {
    }

    public AuthResponseDto(String token, UserResponseDto user) {
        this.token = token;
        this.user = user;
        this.requiresOtp = false;
    }

    public AuthResponseDto(boolean requiresOtp, String email, String mensaje) {
        this.requiresOtp = requiresOtp;
        this.email = email;
        this.mensaje = mensaje;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public UserResponseDto getUser() {
        return user;
    }

    public void setUser(UserResponseDto user) {
        this.user = user;
    }

    public boolean isRequiresOtp() {
        return requiresOtp;
    }

    public void setRequiresOtp(boolean requiresOtp) {
        this.requiresOtp = requiresOtp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
