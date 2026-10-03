package com.sakurastore.backend.domain.model;

import com.sakurastore.backend.domain.exception.DomainException;

public final class StockRules {
    private StockRules() {}

    public static int move(int stock, int quantity, String type) {
        if (quantity <= 0) throw new DomainException("La cantidad debe ser mayor que cero.");
        if (!type.equals("ENTRADA") && !type.equals("SALIDA"))
            throw new DomainException("Tipo de movimiento inválido.");
        long result = (long) stock + (type.equals("ENTRADA") ? quantity : -quantity);
        if (result < 0) throw new DomainException("Stock insuficiente para registrar la salida.");
        if (result > Integer.MAX_VALUE)
            throw new DomainException("La cantidad supera el límite del inventario.");
        return (int) result;
    }
}
