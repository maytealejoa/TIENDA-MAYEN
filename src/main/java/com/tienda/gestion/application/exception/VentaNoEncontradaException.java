package com.tienda.gestion.application.exception;

public class VentaNoEncontradaException extends RuntimeException {
    public VentaNoEncontradaException(Long id) { super("Venta no encontrada: " + id); }
}
