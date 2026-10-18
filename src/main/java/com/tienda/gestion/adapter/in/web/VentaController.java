package com.tienda.gestion.adapter.in.web;

import com.tienda.gestion.application.port.in.VentaUseCase;
import com.tienda.gestion.domain.model.Venta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController @RequestMapping("/api/ventas")
public class VentaController {
    private final VentaUseCase ventas;
    public VentaController(VentaUseCase ventas) { this.ventas = ventas; }
    public record ItemRequest(@NotNull @Positive Long productoId, @NotNull @Positive Integer cantidad) {}
    public record VentaRequest(@NotEmpty @Size(max = 100) List<@NotNull @Valid ItemRequest> items) {}
    @PostMapping
    public ResponseEntity<Venta> registrar(@Valid @RequestBody VentaRequest request, Principal principal) {
        Venta venta = ventas.registrar(request.items().stream().map(i -> new Venta.Item(i.productoId(), i.cantidad())).toList(), principal.getName());
        return ResponseEntity.created(URI.create("/api/ventas/" + venta.id())).body(venta);
    }
    @GetMapping public List<Venta> listar() { return ventas.listar(); }
    @GetMapping("/{id}") public Venta buscar(@PathVariable Long id) { return ventas.buscar(id); }
}
