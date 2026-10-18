package com.tienda.gestion.application.port.in;

import com.tienda.gestion.domain.model.Venta;
import java.util.List;

public interface VentaUseCase {
    Venta registrar(List<Venta.Item> items, String usuario);
    List<Venta> listar();
    Venta buscar(Long id);
}
