package com.tienda.gestion.adapter.in.web;

import com.tienda.gestion.application.port.in.ProductoUseCase;
import com.tienda.gestion.domain.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ProductoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductoUseCase productoUseCase;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);

        ProductoController controller =
                new ProductoController(productoUseCase);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void crearProducto_debeRetornar201() throws Exception {

        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Inca Kola");
        producto.setPrecio(3.50);
        producto.setStock(20);

        when(productoUseCase.crearProducto(org.mockito.ArgumentMatchers.any(Producto.class)))
                .thenReturn(producto);

        String json = """
                {
                    "id": 1,
                    "nombre": "Inca Kola",
                    "precio": 3.50,
                    "stock": 20,
                    "stockMinimo": 5,
                    "fechaVencimiento": "2026-12-15"
                }
                """;

        mockMvc.perform(
                post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nombre").value("Inca Kola"));
    }

    @Test
    void buscarProductoInexistente_debeRetornar404() throws Exception {

        when(productoUseCase.buscarProductoPorId(999L))
                .thenThrow(new com.tienda.gestion.application.exception.ProductoNoEncontradoException(999L));

        mockMvc.perform(
                get("/api/productos/999")
        )
        .andExpect(status().isNotFound());
    }
    @Test
    void stockBajo_debeRetornar200() throws Exception {

        when(productoUseCase.listarProductosStockBajo())
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/productos/stock-bajo")
        )
        .andExpect(
                status().isOk()
        );
    }
    @Test
    void sinStock_debeRetornar200() throws Exception {

        when(productoUseCase.listarProductosSinStock())
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/productos/sin-stock")
        )
        .andExpect(
                status().isOk()
        );
    }
}

