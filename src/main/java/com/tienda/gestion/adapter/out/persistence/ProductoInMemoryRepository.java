package com.tienda.gestion.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.tienda.gestion.application.port.out.ProductoRepository;
import com.tienda.gestion.domain.model.Producto;

// Implementación de referencia de APF1. APF2 utiliza ProductoJpaAdapter.
public class ProductoInMemoryRepository implements ProductoRepository {

    private final List<Producto> productos = new ArrayList<>();
    private Long contador = 1L;

    @Override
    public Producto guardar(Producto producto) {

        producto.setId(contador);
        contador++;

        productos.add(producto);

        return producto;
    }

    @Override
    public List<Producto> listarTodos() {
        return productos;
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {

        return productos.stream()
                .filter(producto -> producto.getId().equals(id))
                .findFirst();
    }

    @Override
    public Producto actualizar(Producto producto) {

        for (int i = 0; i < productos.size(); i++) {

            if (productos.get(i).getId().equals(producto.getId())) {

                productos.set(i, producto);

                return producto;
            }
        }

        return null;
    }

    @Override
    public void eliminar(Long id) {

        productos.removeIf(
                producto -> producto.getId().equals(id)
        );
    }

    @Override
    public List<Producto> buscarStockBajo() {

        return productos.stream()
                .filter(producto ->
                        producto.getStock() <= producto.getStockMinimo())
                .toList();
    }

    @Override
    public List<Producto> buscarSinStock() {

        return productos.stream()
                .filter(producto -> producto.getStock() == 0)
                .toList();
    }
}
