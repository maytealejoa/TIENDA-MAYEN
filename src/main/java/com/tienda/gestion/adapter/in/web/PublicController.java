package com.tienda.gestion.adapter.in.web;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class PublicController {
    @GetMapping("/api/public/ping")
    public Map<String, String> ping() { return Map.of("estado", "OK", "aplicacion", "Tienda MAYEN APF2"); }
}
