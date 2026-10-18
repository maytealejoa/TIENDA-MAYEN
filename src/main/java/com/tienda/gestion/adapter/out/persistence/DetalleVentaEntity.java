package com.tienda.gestion.adapter.out.persistence;

import com.tienda.gestion.domain.model.Venta;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "detalles_venta")
public class DetalleVentaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    VentaEntity venta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    ProductoEntity producto;
    @Column(nullable = false, length = 120)
    String nombreProducto;
    @Column(nullable = false)
    int cantidad;
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal precioUnitario;
    @Column(nullable = false, precision = 24, scale = 2)
    BigDecimal subtotal;
    protected DetalleVentaEntity() {}
    Venta.Detalle dominio() { return new Venta.Detalle(producto.id, nombreProducto, cantidad, precioUnitario, subtotal); }
}
