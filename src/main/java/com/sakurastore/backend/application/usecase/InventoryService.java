package com.sakurastore.backend.application.usecase;

import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.port.InventoryRepositoryPort;
import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.domain.model.StockRules;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {
    private final InventoryRepositoryPort repo;

    public InventoryService(InventoryRepositoryPort repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Category> categories() {
        return repo.categories();
    }

    public Category category(CategoryCommand c) {
        return repo.createCategory(c.name().trim());
    }

    @Transactional(readOnly = true)
    public List<Product> products(String q, boolean low) {
        return repo.products(q, low);
    }

    public Product create(ProductCommand c) {
        validateCategory(c.categoryId());
        return repo.create(c);
    }

    public Product update(Long id, ProductCommand c) {
        find(id);
        validateCategory(c.categoryId());
        return repo.update(id, c);
    }

    public void status(Long id, boolean active) {
        var p = find(id);
        if (!active && p.stock() != 0)
            throw new DomainException("Registra la salida del stock antes de desactivar el producto.");
        repo.setActive(id, active);
    }

    public Movement move(MovementCommand c, Long actorId, String actorName) {
        var p = find(c.productId());
        if (!p.active())
            throw new DomainException("Activa el producto antes de registrar movimientos.");
        int next = StockRules.move(p.stock(), c.quantity(), c.type());
        repo.setStock(p.id(), next);
        return repo.recordMovement(p, c, next, actorId, actorName);
    }

    @Transactional(readOnly = true)
    public List<Movement> movements(Long id, int page) {
        if (page < 0 || page > 100000) throw new DomainException("Página inválida.");
        return repo.movements(id, page);
    }

    private Product find(Long id) {
        return repo.lockProduct(id).orElseThrow(() -> new DomainException("Producto no encontrado."));
    }

    private void validateCategory(Long id) {
        if (!repo.categoryExists(id)) throw new DomainException("La categoría no existe.");
    }
}
