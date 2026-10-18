package com.tienda.gestion.adapter.out.persistence;

import com.tienda.gestion.application.exception.ProductoNoEncontradoException;
import com.tienda.gestion.application.port.out.ProductoRepository;
import com.tienda.gestion.domain.model.Producto;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductoJpaAdapter implements ProductoRepository {
    private final ProductoJpaRepository repository;
    public ProductoJpaAdapter(ProductoJpaRepository repository) { this.repository = repository; }

    public Producto guardar(Producto producto) {
        ProductoEntity entity = new ProductoEntity();
        entity.aplicar(producto);
        return repository.saveAndFlush(entity).dominio();
    }
    public List<Producto> listarTodos() {
        return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(ProductoEntity::dominio).toList();
    }
    public Optional<Producto> buscarPorId(Long id) { return repository.findById(id).map(ProductoEntity::dominio); }
    public Optional<Producto> buscarPorIdParaActualizar(Long id) { return repository.bloquearPorId(id).map(ProductoEntity::dominio); }
    public Producto actualizar(Producto producto) {
        ProductoEntity entity = repository.findById(producto.getId())
                .orElseThrow(() -> new ProductoNoEncontradoException(producto.getId()));
        entity.aplicar(producto);
        return repository.saveAndFlush(entity).dominio();
    }
    public void eliminar(Long id) { repository.deleteById(id); repository.flush(); }
    public List<Producto> buscarStockBajo() { return repository.buscarStockBajo().stream().map(ProductoEntity::dominio).toList(); }
    public List<Producto> buscarSinStock() { return repository.buscarSinStock().stream().map(ProductoEntity::dominio).toList(); }
}
