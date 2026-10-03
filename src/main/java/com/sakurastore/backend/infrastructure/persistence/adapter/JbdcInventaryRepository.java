package com.sakurastore.backend.infrastructure.persistence.adapter;

import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.port.InventoryRepositoryPort;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcInventoryRepository implements InventoryRepositoryPort {

    private final JdbcTemplate db;

    public JdbcInventoryRepository(JdbcTemplate db) {
        this.db = db;
    }

    private static final String SELECT =
            "SELECT p.id_producto, p.codigo, p.nombre, p.id_categoria, " +
                    "c.nombre AS category_name, p.precio, " +
                    "COALESCE(s.stock_minimo, 0) AS min_stock, " +
                    "COALESCE(s.cantidad, 0) AS stock, p.activo " +
                    "FROM producto p " +
                    "JOIN categoria c ON c.id_categoria = p.id_categoria " +
                    "LEFT JOIN stock s ON s.id_producto = p.id_producto ";

    private static final RowMapper<Product> PRODUCT = (r, n) ->
            new Product(
                    r.getLong("id_producto"),
                    r.getString("codigo"),
                    r.getString("nombre"),
                    r.getLong("id_categoria"),
                    r.getString("category_name"),
                    r.getBigDecimal("precio"),
                    r.getInt("min_stock"),
                    r.getInt("stock"),
                    r.getBoolean("activo")
            );

    private static final RowMapper<Movement> MOVEMENT = (r, n) ->
            new Movement(
                    r.getLong("id_movimiento"),
                    r.getLong("id_producto"),
                    r.getString("product_name"),
                    r.getString("tipo"),
                    r.getInt("cantidad"),
                    r.getInt("previous_stock"),
                    r.getInt("new_stock"),
                    r.getString("descripcion"),
                    r.getString("actor_name"),
                    r.getTimestamp("fecha").toLocalDateTime()
            );

    @Override
    public List<Category> categories() {
        return db.query(
                "SELECT id_categoria, nombre " +
                        "FROM categoria " +
                        "WHERE activo = true " +
                        "ORDER BY nombre",
                (r, n) -> new Category(
                        r.getLong("id_categoria"),
                        r.getString("nombre")
                )
        );
    }

    @Override
    public Category createCategory(String name) {
        return db.queryForObject(
                "INSERT INTO categoria(nombre, descripcion, activo) " +
                        "VALUES (?, NULL, true) " +
                        "RETURNING id_categoria, nombre",
                (r, n) -> new Category(
                        r.getLong("id_categoria"),
                        r.getString("nombre")
                ),
                name.trim()
        );
    }

    @Override
    public List<Product> products(String query, boolean lowStock) {
        String search = "%" + (query == null ? "" : query) + "%";

        String condition =
                "WHERE (p.nombre ILIKE ? " +
                        "OR p.codigo ILIKE ? " +
                        "OR c.nombre ILIKE ?) ";

        if (lowStock) {
            condition +=
                    "AND COALESCE(s.cantidad, 0) <= COALESCE(s.stock_minimo, 0) " +
                            "AND p.activo = true ";
        }

        return db.query(
                SELECT + condition + "ORDER BY p.id_producto DESC",
                PRODUCT,
                search,
                search,
                search
        );
    }

    @Override
    public Optional<Product> lockProduct(Long id) {
        return db.query(
                SELECT + "WHERE p.id_producto = ? FOR UPDATE OF p",
                PRODUCT,
                id
        ).stream().findFirst();
    }

    private Product find(Long id) {
        return db.queryForObject(
                SELECT + "WHERE p.id_producto = ?",
                PRODUCT,
                id
        );
    }

    @Override
    public Product create(ProductCommand command) {
        Long id = db.queryForObject(
                "INSERT INTO producto " +
                        "(codigo, nombre, descripcion, precio, activo, id_categoria) " +
                        "VALUES (?, ?, NULL, ?, true, ?) " +
                        "RETURNING id_producto",
                Long.class,
                command.sku().trim().toUpperCase(Locale.ROOT),
                command.name().trim(),
                command.price(),
                command.categoryId()
        );

        db.update(
                "INSERT INTO stock(id_producto, cantidad, stock_minimo) " +
                        "VALUES (?, ?, ?)",
                id,
                0,
                command.minStock()
        );

        return find(id);
    }

    @Override
    public Product update(Long id, ProductCommand command) {
        db.update(
                "UPDATE producto SET " +
                        "codigo = ?, nombre = ?, id_categoria = ?, precio = ? " +
                        "WHERE id_producto = ?",
                command.sku().trim().toUpperCase(Locale.ROOT),
                command.name().trim(),
                command.categoryId(),
                command.price(),
                id
        );

        db.update(
                "UPDATE stock SET stock_minimo = ? " +
                        "WHERE id_producto = ?",
                command.minStock(),
                id
        );

        return find(id);
    }

    @Override
    public void setActive(Long id, boolean active) {
        db.update(
                "UPDATE producto SET activo = ? " +
                        "WHERE id_producto = ?",
                active,
                id
        );
    }

    @Override
    public boolean categoryExists(Long id) {
        return Boolean.TRUE.equals(
                db.queryForObject(
                        "SELECT EXISTS(" +
                                "SELECT 1 FROM categoria " +
                                "WHERE id_categoria = ? AND activo = true)",
                        Boolean.class,
                        id
                )
        );
    }

    @Override
    public void setStock(Long id, int stock) {
        db.update(
                "UPDATE stock SET cantidad = ? " +
                        "WHERE id_producto = ?",
                stock,
                id
        );
    }

    @Override
    @Transactional
    public Movement recordMovement(
            Product product,
            MovementCommand command,
            int stock,
            Long actorId,
            String actorName
    ) {
        Long id = db.queryForObject(
                "INSERT INTO movimiento_inventario " +
                        "(id_producto, id_usuario, tipo, cantidad, fecha, descripcion) " +
                        "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?) " +
                        "RETURNING id_movimiento",
                Long.class,
                product.id(),
                actorId,
                command.type(),
                command.quantity(),
                command.reason().trim()
        );

        db.update(
                "UPDATE stock SET cantidad = ? " +
                        "WHERE id_producto = ?",
                stock,
                product.id()
        );

        return db.queryForObject(
                "SELECT m.*, " +
                        "p.nombre AS product_name, " +
                        "? AS previous_stock, " +
                        "? AS new_stock, " +
                        "COALESCE(u.nombre, 'Sistema') AS actor_name " +
                        "FROM movimiento_inventario m " +
                        "JOIN producto p ON p.id_producto = m.id_producto " +
                        "LEFT JOIN usuario u ON u.id_usuario = m.id_usuario " +
                        "WHERE m.id_movimiento = ?",
                MOVEMENT,
                product.stock(),
                stock,
                id
        );
    }

    @Override
    public List<Movement> movements(Long productId, int page) {
        String sql =
                "SELECT m.*, " +
                        "p.nombre AS product_name, " +
                        "0 AS previous_stock, " +
                        "0 AS new_stock, " +
                        "COALESCE(u.nombre, 'Sistema') AS actor_name " +
                        "FROM movimiento_inventario m " +
                        "JOIN producto p ON p.id_producto = m.id_producto " +
                        "LEFT JOIN usuario u ON u.id_usuario = m.id_usuario ";

        if (productId == null) {
            return db.query(
                    sql +
                            "ORDER BY m.id_movimiento DESC " +
                            "LIMIT 50 OFFSET ?",
                    MOVEMENT,
                    (long) page * 50
            );
        }

        return db.query(
                sql +
                        "WHERE m.id_producto = ? " +
                        "ORDER BY m.id_movimiento DESC " +
                        "LIMIT 50 OFFSET ?",
                MOVEMENT,
                productId,
                (long) page * 50
        );
    }
}
