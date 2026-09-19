package com.sakurastore.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyEmailCommand {

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido.")
    private String email;

    @NotBlank(message = "El código de verificación es obligatorio.")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe ser de 6 dígitos numéricos.")
    private String codigo;

    public VerifyEmailCommand() {
    }

    public VerifyEmailCommand(String email, String codigo) {
        this.email = email;
        this.codigo = codigo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
