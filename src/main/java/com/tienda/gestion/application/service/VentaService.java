package com.tienda.gestion.application.service;

import com.tienda.gestion.application.exception.*;
import com.tienda.gestion.application.port.in.VentaUseCase;
import com.tienda.gestion.application.port.out.*;
import com.tienda.gestion.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
public class VentaService implements VentaUseCase {
    private final ProductoRepository productos;
    private final VentaRepository ventas;
    public VentaService(ProductoRepository productos, VentaRepository ventas) {
        this.productos = productos; this.ventas = ventas;
    }

    @Transactional
    public Venta registrar(List<Venta.Item> items, String usuario) {
        if (items == null || items.isEmpty() || items.size() > 100) {
            throw new IllegalArgumentException("La venta requiere entre 1 y 100 productos");
        }
        Set<Long> ids = new HashSet<>();
        for (Venta.Item item : items) {
            if (item == null || item.productoId() == null || item.productoId() <= 0 || item.cantidad() <= 0) {
                throw new IllegalArgumentException("Producto y cantidad deben ser positivos");
            }
            if (!ids.add(item.productoId())) throw new IllegalArgumentException("No repetir productos en una venta");
        }
        List<Venta.Detalle> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        // Bloquear siempre en el mismo orden evita ciclos de bloqueo entre ventas simultáneas.
        for (Venta.Item item : items.stream().sorted(Comparator.comparing(Venta.Item::productoId)).toList()) {
            Producto producto = productos.buscarPorIdParaActualizar(item.productoId())
                    .orElseThrow(() -> new ProductoNoEncontradoException(item.productoId()));
            if (producto.getStock() < item.cantidad()) {
                throw new ConflictoInventarioException("Stock insuficiente para " + producto.getNombre());
            }
            BigDecimal precio = BigDecimal.valueOf(producto.getPrecio()).setScale(2);
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.cantidad()));
            producto.setStock(producto.getStock() - item.cantidad());
            productos.actualizar(producto);
            detalles.add(new Venta.Detalle(producto.getId(), producto.getNombre(), item.cantidad(), precio, subtotal));
            total = total.add(subtotal);
        }
        return ventas.guardar(new Venta(null, LocalDateTime.now(), usuario, total, List.copyOf(detalles)));
    }
    public List<Venta> listar() { return ventas.listar(); }
    public Venta buscar(Long id) { return ventas.buscar(id).orElseThrow(() -> new VentaNoEncontradaException(id)); }
}
