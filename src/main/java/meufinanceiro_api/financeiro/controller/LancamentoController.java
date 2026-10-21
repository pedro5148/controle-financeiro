package meufinanceiro_api.financeiro.controller;

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

@RestController
@RequestMapping("/api/lancamentos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LancamentoController {

    private final LancamentoService lancamentoService;

    @GetMapping
    public Page<LancamentoResponseDTO> listar(
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(required = false) StatusLancamento status,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long subcategoriaId,
            @RequestParam(required = false) Long grupoParcelamentoId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @PageableDefault(sort = "dataLancamento", direction = Sort.Direction.DESC, size = 20) Pageable pageable) {

        return lancamentoService.listar(
                descricao, tipo, status, contaId, categoriaId, subcategoriaId, grupoParcelamentoId, mes, ano, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LancamentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(lancamentoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<List<LancamentoResponseDTO>> criar(@Valid @RequestBody LancamentoRequestDTO dto) {
        List<LancamentoResponseDTO> criados = lancamentoService.criarLancamentos(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criados);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LancamentoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody LancamentoUpdateDTO dto) {
        return ResponseEntity.ok(lancamentoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        lancamentoService.excluir(id);
    }
}
