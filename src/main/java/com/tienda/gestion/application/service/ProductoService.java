package com.tienda.gestion.application.service;

import com.tienda.gestion.application.exception.ProductoNoEncontradoException;
import com.tienda.gestion.application.port.in.ProductoUseCase;
import com.tienda.gestion.application.port.out.ProductoRepository;
import com.tienda.gestion.domain.model.Producto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;

import java.util.List;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('LECTOR', 'EDITOR', 'ADMIN')")
public class ProductoService implements ProductoUseCase {

    private final ProductoRepository productoRepository;
    

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public Producto crearProducto(Producto producto) {

        validarProducto(producto);
        producto.setId(null); // El identificador de una creación lo genera la base de datos.

        return productoRepository.guardar(producto);
    }

    @Override
    public List<Producto> listarProductos() {
        return productoRepository.listarTodos();
    }

    @Override
    public Producto buscarProductoPorId(Long id) {

        return productoRepository.buscarPorId(id)
                .orElseThrow(
                    () -> new ProductoNoEncontradoException(id)
                );
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public Producto actualizarProducto(Long id, Producto producto) {

        productoRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));

        validarProducto(producto);

        producto.setId(id);

        return productoRepository.actualizar(producto);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminarProducto(Long id) {

        productoRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));

        productoRepository.eliminar(id);
    }
    
    private void validarProducto(Producto producto) {

        if (producto == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }

        if (producto.getNombre() == null ||
            producto.getNombre().isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre del producto es obligatorio"
            );
        }

        if (producto.getNombre().length() > 120) {
            throw new IllegalArgumentException("El nombre admite hasta 120 caracteres");
        }
        if (!Double.isFinite(producto.getPrecio()) || producto.getPrecio() <= 0
                || producto.getPrecio() > 9999999999.99
                || BigDecimal.valueOf(producto.getPrecio()).stripTrailingZeros().scale() > 2) {

            throw new IllegalArgumentException(
                    "El precio debe ser positivo, con hasta 10 enteros y 2 decimales"
            );
        }

        if (producto.getStock() < 0) {

            throw new IllegalArgumentException(
                    "El stock no puede ser negativo"
            );
        }

        if (producto.getFechaVencimiento() == null) {

            throw new IllegalArgumentException(
                    "La fecha de vencimiento es obligatoria"
            );
        }
        if (producto.getStockMinimo() < 0) {

            throw new IllegalArgumentException(
                    "El stock mínimo no puede ser negativo"
            );
        }
    }
    @Override
    public List<Producto> listarProductosStockBajo() {

        return productoRepository.buscarStockBajo();
    }
    @Override
    public List<Producto> listarProductosSinStock() {

        return productoRepository.buscarSinStock();
    }
}
