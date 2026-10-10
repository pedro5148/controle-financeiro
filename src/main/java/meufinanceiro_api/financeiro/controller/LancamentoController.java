package meufinanceiro_api.financeiro.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
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
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @PageableDefault(sort = "dataEfetivacao", direction = Sort.Direction.DESC, size = 20) Pageable pageable) {

        return lancamentoService.listar(descricao, tipo, mes, ano, pageable);
    }

    @PostMapping
    public ResponseEntity<List<LancamentoResponseDTO>> criar(@Valid @RequestBody LancamentoRequestDTO dto) {
        List<LancamentoResponseDTO> criados = lancamentoService.criarLancamentos(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criados);
    }

    @PutMapping("/{id}")
    public LancamentoResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody LancamentoRequestDTO dto) {
        return lancamentoService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        lancamentoService.excluir(id);
    }
}
