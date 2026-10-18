package com.tienda.gestion.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Venta(Long id, LocalDateTime fecha, String usuario, BigDecimal total, List<Detalle> detalles) {
    public record Detalle(Long productoId, String nombreProducto, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}
    public record Item(Long productoId, int cantidad) {}
}
