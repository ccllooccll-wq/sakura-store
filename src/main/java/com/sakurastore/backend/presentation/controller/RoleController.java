package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.domain.model.Role;
import com.sakurastore.backend.domain.port.RoleRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    private final RoleRepositoryPort roleRepositoryPort;

    public RoleController(RoleRepositoryPort roleRepositoryPort) {
        this.roleRepositoryPort = roleRepositoryPort;
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleRepositoryPort.findAll());
    }
}
