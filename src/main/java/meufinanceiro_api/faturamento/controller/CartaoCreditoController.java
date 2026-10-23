package meufinanceiro_api.faturamento.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.faturamento.dto.CartaoCreditoRequestDTO;
import meufinanceiro_api.faturamento.dto.CartaoCreditoResponseDTO;
import meufinanceiro_api.faturamento.service.CartaoCreditoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Cartões de Crédito", description = "Endpoints para gerenciamento de cartões de crédito e limites")
@RestController
@RequestMapping("/api/cartoes-credito")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CartaoCreditoController {

    private final CartaoCreditoService service;

    @Operation(summary = "Listar cartões de crédito", description = "Retorna a lista de cartões cadastrados ordenados com o cartão principal no topo. Permite filtrar apenas ativos.")
    @GetMapping
    public List<CartaoCreditoResponseDTO> listarTodos(@RequestParam(required = false) Boolean apenasAtivos) {
        return service.listarTodos(apenasAtivos);
    }

    @Operation(summary = "Buscar cartão de crédito por ID", description = "Retorna os detalhes e limites de um cartão de crédito específico.")
    @GetMapping("/{id}")
    public CartaoCreditoResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Criar novo cartão de crédito", description = "Cadastra um novo cartão de crédito. Se for o primeiro cartão do sistema, ele é automaticamente definido como principal.")
    @PostMapping
    public ResponseEntity<CartaoCreditoResponseDTO> criar(@Valid @RequestBody CartaoCreditoRequestDTO dto) {
        CartaoCreditoResponseDTO resposta = service.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @Operation(summary = "Atualizar cartão de crédito", description = "Atualiza os dados do cartão de crédito (nome, limite, dias de fechamento/vencimento, conta padrão, bandeira, principal e ativo).")
    @PutMapping("/{id}")
    public CartaoCreditoResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody CartaoCreditoRequestDTO dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Inativar cartão de crédito", description = "Desativa o cartão de crédito para que não possa ser selecionado em novos lançamentos.")
    @PatchMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @Operation(summary = "Ativar cartão de crédito", description = "Reativa um cartão de crédito previamente inativado.")
    @PatchMapping("/{id}/ativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ativar(@PathVariable Long id) {
        service.ativar(id);
    }

    @Operation(summary = "Excluir cartão de crédito", description = "Remove fisicamente o cartão de crédito caso não possua faturas ou lançamentos vinculados e não seja o cartão principal.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}

