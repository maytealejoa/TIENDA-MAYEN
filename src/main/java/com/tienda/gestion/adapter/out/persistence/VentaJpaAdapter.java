package com.tienda.gestion.adapter.out.persistence;

import com.tienda.gestion.application.port.out.VentaRepository;
import com.tienda.gestion.domain.model.Venta;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class VentaJpaAdapter implements VentaRepository {
    private final VentaJpaRepository ventas;
    private final ProductoJpaRepository productos;
    public VentaJpaAdapter(VentaJpaRepository ventas, ProductoJpaRepository productos) {
        this.ventas = ventas; this.productos = productos;
    }
    public Venta guardar(Venta v) {
        VentaEntity entity = new VentaEntity();
        entity.fecha = v.fecha(); entity.usuario = v.usuario(); entity.total = v.total();
        for (Venta.Detalle d : v.detalles()) {
            DetalleVentaEntity detalle = new DetalleVentaEntity();
            detalle.venta = entity; detalle.producto = productos.getReferenceById(d.productoId());
            detalle.nombreProducto = d.nombreProducto(); detalle.cantidad = d.cantidad();
            detalle.precioUnitario = d.precioUnitario(); detalle.subtotal = d.subtotal();
            entity.detalles.add(detalle);
        }
        return ventas.saveAndFlush(entity).dominio();
    }
    public List<Venta> listar() { return ventas.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(VentaEntity::dominio).toList(); }
    public Optional<Venta> buscar(Long id) { return ventas.findById(id).map(VentaEntity::dominio); }
}
