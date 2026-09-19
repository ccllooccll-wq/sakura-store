package com.sakurastore.backend.application.dto;

public class ApiResponseDto {
    private String mensaje;

    public ApiResponseDto() {
    }

    public ApiResponseDto(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
