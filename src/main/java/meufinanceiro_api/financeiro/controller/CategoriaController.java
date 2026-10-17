package meufinanceiro_api.financeiro.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.dto.CategoriaRequestDTO;
import meufinanceiro_api.financeiro.dto.CategoriaResponseDTO;
import meufinanceiro_api.financeiro.dto.CategoriaUpdateDTO;
import meufinanceiro_api.financeiro.service.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Categorias", description = "Endpoints para gerenciamento de categorias e suas subcategorias")
@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @Operation(summary = "Listar categorias", description = "Retorna a lista de todas as categorias cadastradas em ordem alfabética, incluindo suas subcategorias aninhadas. Permite filtrar apenas ativas.")
    @GetMapping
    public List<CategoriaResponseDTO> listarTodasCategorias(@RequestParam(required = false) Boolean apenasAtivas){
        return service.listarTodasCategorias(apenasAtivas);
    }

    @Operation(summary = "Buscar categoria por ID", description = "Retorna os detalhes de uma categoria específica e suas subcategorias informada pelo identificador único.")
    @GetMapping("/{id}")
    public CategoriaResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Criar nova categoria", description = "Cadastra uma nova categoria. Opcionalmente, permite criar junto uma primeira subcategoria filha informando o atributo subcategoriaNome.")
    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> criar(@Valid @RequestBody CategoriaRequestDTO dto) {
        CategoriaResponseDTO resposta = service.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @Operation(summary = "Atualizar categoria", description = "Atualiza o nome da categoria identificada pelo ID.")
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaUpdateDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @Operation(summary = "Inativar categoria", description = "Desativa a categoria para que não possa ser mais selecionada em novos lançamentos financeiros.")
    @PatchMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @Operation(summary = "Ativar categoria", description = "Reativa uma categoria previamente inativada.")
    @PatchMapping("/{id}/ativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ativar(@PathVariable Long id) {
        service.ativar(id);
    }

    @Operation(summary = "Excluir categoria", description = "Remove fisicamente a categoria e suas subcategorias associadas caso não possua lançamentos financeiros vinculados.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
