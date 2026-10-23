package meufinanceiro_api.financeiro.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
import meufinanceiro_api.financeiro.dto.LancamentoUpdateDTO;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.service.LancamentoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lançamentos", description = "Endpoints para gerenciamento de lançamentos financeiros (receitas, despesas e transferências)")
@RestController
@RequestMapping("/api/lancamentos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LancamentoController {

    private final LancamentoService lancamentoService;

    @Operation(summary = "Listar lançamentos", description = "Retorna uma página de lançamentos ordenada por data de lançamento decrescente, permitindo filtros combinados.")
    @GetMapping
    public ResponseEntity<Page<LancamentoResponseDTO>> listar(
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(required = false) StatusLancamento status,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long subcategoriaId,
            @RequestParam(required = false) Long grupoParcelamentoId,
            @RequestParam(required = false) Long cartaoCreditoId,
            @RequestParam(required = false) Long faturaId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @PageableDefault(sort = "dataLancamento", direction = Sort.Direction.DESC, size = 20) Pageable pageable) {

        return ResponseEntity.ok(lancamentoService.listar(
                descricao, tipo, status, contaId, categoriaId, subcategoriaId, grupoParcelamentoId, cartaoCreditoId, faturaId, mes, ano, pageable));
    }

    @Operation(summary = "Buscar lançamento por ID", description = "Retorna os detalhes de um lançamento específico informado pelo seu identificador único.")
    @GetMapping("/{id}")
    public ResponseEntity<LancamentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(lancamentoService.buscarPorId(id));
    }

    @Operation(summary = "Criar lançamento(s)", description = "Cadastra um lançamento avulso ou gera múltiplos lançamentos mensais quando o tipo de recorrência for PARCELADO.")
    @PostMapping
    public ResponseEntity<List<LancamentoResponseDTO>> criar(@Valid @RequestBody LancamentoRequestDTO dto) {
        List<LancamentoResponseDTO> criados = lancamentoService.criarLancamentos(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criados);
    }

    @Operation(summary = "Atualizar lançamento", description = "Atualiza um lançamento individual existente, realizando o estorno financeiro automático do estado anterior e aplicando o novo impacto no saldo.")
    @PutMapping("/{id}")
    public ResponseEntity<LancamentoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody LancamentoUpdateDTO dto) {
        return ResponseEntity.ok(lancamentoService.atualizar(id, dto));
    }

    @Operation(summary = "Excluir lançamento", description = "Remove fisicamente o lançamento e reverte seu impacto financeiro no saldo da conta caso estivesse efetivado. Se excluirPendentesDoGrupo=true em um lançamento parcelado, exclui também todas as demais parcelas pendentes do mesmo grupo.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean excluirPendentesDoGrupo) {
        lancamentoService.excluir(id, excluirPendentesDoGrupo);
    }
}
