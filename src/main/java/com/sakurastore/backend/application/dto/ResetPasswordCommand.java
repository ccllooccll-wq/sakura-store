package com.sakurastore.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordCommand {

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Formato de correo electrónico inválido.")
    private String email;

    @NotBlank(message = "El código de verificación es obligatorio.")
    @Pattern(regexp = "^[0-9]{6}$", message = "El código debe ser de 6 dígitos numéricos.")
    private String codigo;

    @NotBlank(message = "La nueva contraseña es obligatoria.")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.")
    private String nuevaPassword;

    public ResetPasswordCommand() {
    }

    public ResetPasswordCommand(String email, String codigo, String nuevaPassword) {
        this.email = email;
        this.codigo = codigo;
        this.nuevaPassword = nuevaPassword;
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

    public String getNuevaPassword() {
        return nuevaPassword;
    }

    public void setNuevaPassword(String nuevaPassword) {
        this.nuevaPassword = nuevaPassword;
    }
}
