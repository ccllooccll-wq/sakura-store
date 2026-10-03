package com.sakurastore.backend.application.port;

import com.sakurastore.backend.application.dto.InventoryDtos.*;
import java.util.List;
import java.util.Optional;

public interface InventoryRepositoryPort {
    List<Category> categories();

    Category createCategory(String name);

    List<Product> products(String query, boolean lowStock);

    Optional<Product> lockProduct(Long id);

    Product create(ProductCommand command);

    Product update(Long id, ProductCommand command);

    void setActive(Long id, boolean active);

    boolean categoryExists(Long id);

    void setStock(Long id, int stock);

    Movement recordMovement(
            Product product, MovementCommand command, int stock, Long actorId, String actorName);

    List<Movement> movements(Long productId, int page);
}