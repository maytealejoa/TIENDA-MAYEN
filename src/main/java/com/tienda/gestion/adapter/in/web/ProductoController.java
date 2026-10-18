package com.tienda.gestion.adapter.in.web;

import com.tienda.gestion.application.port.in.ProductoUseCase;
import com.tienda.gestion.domain.model.Producto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoUseCase productoUseCase;

    public ProductoController(ProductoUseCase productoUseCase) {
        this.productoUseCase = productoUseCase;
    }

    @PostMapping
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody ProductoRequest request) {
        Producto producto = productoUseCase.crearProducto(request.dominio());
        return ResponseEntity.created(URI.create("/api/productos/" + producto.getId())).body(producto);
    }

    @GetMapping
    public List<Producto> listarProductos() {
        return productoUseCase.listarProductos();
    }

    @GetMapping("/{id}")
    public Producto buscarProductoPorId(@PathVariable Long id) {

        return productoUseCase.buscarProductoPorId(id);
    }
    @PutMapping("/{id}")
    public Producto actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest producto) {

        return productoUseCase.actualizarProducto(id, producto.dominio());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarProducto(@PathVariable Long id) {
        productoUseCase.eliminarProducto(id);
    }
    @GetMapping("/stock-bajo")
    public List<Producto> listarProductosStockBajo() {

        return productoUseCase.listarProductosStockBajo();
    }
    @GetMapping("/sin-stock")
    public List<Producto> listarProductosSinStock() {

        return productoUseCase.listarProductosSinStock();
    }
}
