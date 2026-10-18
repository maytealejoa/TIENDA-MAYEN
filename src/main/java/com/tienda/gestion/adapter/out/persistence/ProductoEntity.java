package com.tienda.gestion.adapter.out.persistence;

import com.tienda.gestion.domain.model.Producto;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "productos")
public class ProductoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 120)
    String nombre;
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal precio;
    @Column(nullable = false)
    int stock;
    @Column(nullable = false)
    int stockMinimo;
    @Column(nullable = false)
    LocalDate fechaVencimiento;

    protected ProductoEntity() {}

    void aplicar(Producto p) {
        nombre = p.getNombre();
        precio = BigDecimal.valueOf(p.getPrecio());
        stock = p.getStock();
        stockMinimo = p.getStockMinimo();
        fechaVencimiento = p.getFechaVencimiento();
    }

    Producto dominio() {
        Producto p = new Producto();
        p.setId(id); p.setNombre(nombre); p.setPrecio(precio.doubleValue());
        p.setStock(stock); p.setStockMinimo(stockMinimo); p.setFechaVencimiento(fechaVencimiento);
        return p;
    }
}
