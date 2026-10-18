package meufinanceiro_api.financeiro.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.dto.SubcategoriaRequestDTO;
import meufinanceiro_api.financeiro.dto.SubcategoriaResponseDTO;
import meufinanceiro_api.financeiro.dto.SubcategoriaUpdateDTO;
import meufinanceiro_api.financeiro.service.SubcategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Subcategorias", description = "Endpoints para gerenciamento de subcategorias vinculadas às categorias")
@RestController
@RequestMapping("/api/subcategorias")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubcategoriaController {

    private final SubcategoriaService service;

    @Operation(summary = "Listar subcategorias", description = "Retorna a lista de subcategorias ordenadas alfabeticamente. Permite filtrar por categoria pai e apenas ativas.")
    @GetMapping
    public List<SubcategoriaResponseDTO> listarSubcategorias(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean apenasAtivas) {
        return service.listarSubcategorias(categoriaId, apenasAtivas);
    }

    @Operation(summary = "Buscar subcategoria por ID", description = "Retorna os detalhes de uma subcategoria específica informada pelo identificador único.")
    @GetMapping("/{id}")
    public SubcategoriaResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Criar nova subcategoria", description = "Cadastra uma nova subcategoria vinculada a uma categoria pai ativa.")
    @PostMapping
    public ResponseEntity<SubcategoriaResponseDTO> criar(@Valid @RequestBody SubcategoriaRequestDTO dto) {
        SubcategoriaResponseDTO resposta = service.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @Operation(summary = "Atualizar subcategoria", description = "Atualiza exclusivamente o nome da subcategoria identificada pelo ID.")
    @PutMapping("/{id}")
    public SubcategoriaResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody SubcategoriaUpdateDTO dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Inativar subcategoria", description = "Desativa a subcategoria para que não possa ser mais selecionada em novos lançamentos financeiros.")
    @PatchMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @Operation(summary = "Ativar subcategoria", description = "Reativa uma subcategoria previamente inativada.")
    @PatchMapping("/{id}/ativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ativar(@PathVariable Long id) {
        service.ativar(id);
    }

    @Operation(summary = "Excluir subcategoria", description = "Remove fisicamente a subcategoria caso não possua lançamentos financeiros vinculados.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
