package meufinanceiro_api.financeiro.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.dto.SubcategoriaRequestDTO;
import meufinanceiro_api.financeiro.dto.SubcategoriaResponseDTO;
import meufinanceiro_api.financeiro.service.SubCategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subcategorias")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubCategoriaController {

    private final SubCategoriaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubcategoriaResponseDTO criar(@Valid @RequestBody SubcategoriaRequestDTO dto) {
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public SubcategoriaResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody SubcategoriaRequestDTO dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
