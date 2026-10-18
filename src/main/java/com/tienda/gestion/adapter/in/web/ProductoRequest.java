package com.tienda.gestion.adapter.in.web;

import com.tienda.gestion.domain.model.Producto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal precio,
        @NotNull @Min(0) Integer stock,
        @NotNull @Min(0) Integer stockMinimo,
        @NotNull LocalDate fechaVencimiento) {
    Producto dominio() {
        Producto p = new Producto();
        p.setNombre(nombre); p.setPrecio(precio.doubleValue()); p.setStock(stock);
        p.setStockMinimo(stockMinimo); p.setFechaVencimiento(fechaVencimiento);
        return p;
    }
}
