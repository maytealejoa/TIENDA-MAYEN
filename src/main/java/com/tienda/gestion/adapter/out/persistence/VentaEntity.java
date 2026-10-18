package com.tienda.gestion.adapter.out.persistence;

import com.tienda.gestion.domain.model.Venta;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "ventas")
public class VentaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    LocalDateTime fecha;
    @Column(nullable = false, length = 60)
    String usuario;
    @Column(nullable = false, precision = 24, scale = 2)
    BigDecimal total;
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    @OrderBy("id asc")
    List<DetalleVentaEntity> detalles = new ArrayList<>();
    protected VentaEntity() {}
    Venta dominio() { return new Venta(id, fecha, usuario, total, detalles.stream().map(DetalleVentaEntity::dominio).toList()); }
}
