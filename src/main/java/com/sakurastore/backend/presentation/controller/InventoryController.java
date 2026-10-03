package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.usecase.InventoryService;
import com.sakurastore.backend.infrastructure.security.SessionIdentity;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return inventory.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public Category category(@Valid @RequestBody CategoryCommand c) {
        return inventory.category(c);
    }

    @GetMapping("/products")
    public List<Product> products(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "false") boolean lowStock) {
        return inventory.products(q, lowStock);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductCommand c) {
        return inventory.create(c);
    }

    @PutMapping("/products/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductCommand c) {
        return inventory.update(id, c);
    }

    @PatchMapping("/products/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void status(@PathVariable Long id, @Valid @RequestBody StatusCommand c) {
        inventory.status(id, c.active());
    }

    @GetMapping("/movements")
    public List<Movement> movements(
            @RequestParam(required = false) Long productId, @RequestParam(defaultValue = "0") int page) {
        return inventory.movements(productId, page);
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public Movement move(@Valid @RequestBody MovementCommand c, Authentication auth) {
        var identity = (SessionIdentity) auth.getPrincipal();
        return inventory.move(c, identity.id(), identity.username());
    }
}
