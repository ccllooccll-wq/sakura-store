package com.sakurastore.backend.domain.model;

import com.sakurastore.backend.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Debe crear un usuario válido correctamente")
    void shouldCreateValidUser() {
        Role role = new Role(1L, RoleEnum.ROLE_ADMIN, "Administrador del sistema");
        User user = User.createNewUser("sakura_admin", "Admin Sakura", "admin@sakurastore.com", "Password123!", role);

        assertNotNull(user);
        assertEquals("sakura_admin", user.getUsername());
        assertEquals("admin@sakurastore.com", user.getEmail());
        assertTrue(user.isActive());
        assertEquals(RoleEnum.ROLE_ADMIN, user.getRole().getName());
    }

    @Test
    @DisplayName("Debe lanzar DomainException si el correo es inválido")
    void shouldFailWhenEmailIsInvalid() {
        Role role = new Role(1L, RoleEnum.ROLE_VENDEDOR, "Vendedor");
        
        DomainException exception = assertThrows(DomainException.class, () -> {
            User.createNewUser("vendedor1", "Vendedor Uno", "correo-invalido", "Password123!", role);
        });

        assertTrue(exception.getMessage().contains("formato del correo electrónico es inválido"));
    }

    @Test
    @DisplayName("Debe lanzar DomainException si el username es muy corto")
    void shouldFailWhenUsernameIsTooShort() {
        Role role = new Role(1L, RoleEnum.ROLE_VENDEDOR, "Vendedor");

        assertThrows(DomainException.class, () -> {
            User.createNewUser("ab", "Vendedor Uno", "vendedor@sakurastore.com", "Password123!", role);
        });
    }

    @Test
    @DisplayName("Debe cambiar el estado del usuario mediante activar y desactivar")
    void shouldToggleUserStatus() {
        Role role = new Role(1L, RoleEnum.ROLE_ALMACENERO, "Almacenero");
        User user = User.createNewUser("almacen1", "Jose Almacen", "almacen@sakurastore.com", "Password123!", role);

        assertTrue(user.isActive());
        user.desactivar();
        assertFalse(user.isActive());

        user.activar();
        assertTrue(user.isActive());
    }
}
