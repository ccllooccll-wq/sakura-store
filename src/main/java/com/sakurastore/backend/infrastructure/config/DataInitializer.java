package com.sakurastore.backend.infrastructure.config;

import com.sakurastore.backend.application.port.PasswordEncoderPort;
import com.sakurastore.backend.domain.model.Role;
import com.sakurastore.backend.domain.model.RoleEnum;
import com.sakurastore.backend.domain.model.User;
import com.sakurastore.backend.domain.port.RoleRepositoryPort;
import com.sakurastore.backend.domain.port.UserRepositoryPort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepositoryPort roleRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public DataInitializer(RoleRepositoryPort roleRepositoryPort, UserRepositoryPort userRepositoryPort, PasswordEncoderPort passwordEncoderPort) {
        this.roleRepositoryPort = roleRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public void run(String... args) {
        Role adminRole = roleRepositoryPort.findByName(RoleEnum.ROLE_ADMIN)
                .orElseGet(() -> roleRepositoryPort.save(new Role(RoleEnum.ROLE_ADMIN, "Administrador del Sistema Sakura Store")));

        roleRepositoryPort.findByName(RoleEnum.ROLE_VENDEDOR)
                .orElseGet(() -> roleRepositoryPort.save(new Role(RoleEnum.ROLE_VENDEDOR, "Vendedor de tienda")));

        roleRepositoryPort.findByName(RoleEnum.ROLE_ALMACENERO)
                .orElseGet(() -> roleRepositoryPort.save(new Role(RoleEnum.ROLE_ALMACENERO, "Almacenero / Gestión de inventario")));

        if (!userRepositoryPort.existsByUsername("admin")) {
            String encodedPassword = passwordEncoderPort.encode("admin123");
            User adminUser = User.createNewUser(
                    "admin",
                    "Administrador Sakura Store",
                    "admin@sakurastore.com",
                    encodedPassword,
                    adminRole
            );
            adminUser.markEmailAsVerified();
            userRepositoryPort.save(adminUser);
            System.out.println(">>> [Sakura Store] Usuario administrador inicial creado: username='admin', password='admin123'");
        }
    }
}
