package com.tienda.gestion.application.port.out;

import com.tienda.gestion.domain.model.Venta;
import java.util.List;
import java.util.Optional;

public interface VentaRepository {
    Venta guardar(Venta venta);
    List<Venta> listar();
    Optional<Venta> buscar(Long id);
}
