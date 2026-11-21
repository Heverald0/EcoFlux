package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.response.MaterialResposta;
import com.example.EcoFlux.entity.enums.TipoMaterial;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/materiais")
public class MaterialControlador {

    @GetMapping
    public ResponseEntity<List<MaterialResposta>> listarTodos() {
        List<MaterialResposta> materiais = Arrays.stream(TipoMaterial.values())
                .map(MaterialResposta::converter)
                .toList();
        return ResponseEntity.ok(materiais);
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<MaterialResposta> buscarPorCodigo(@PathVariable String codigo) {
        TipoMaterial material = TipoMaterial.valueOf(codigo.trim().toUpperCase());
        return ResponseEntity.ok(MaterialResposta.converter(material));
    }
}