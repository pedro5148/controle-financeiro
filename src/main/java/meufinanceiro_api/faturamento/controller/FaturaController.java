package meufinanceiro_api.faturamento.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.faturamento.dto.FaturaResponseDTO;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import meufinanceiro_api.faturamento.service.FaturaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Faturas", description = "Endpoints para consulta de faturas de cartão de crédito")
@RestController
@RequestMapping("/api/faturas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FaturaController {

    private final FaturaService faturaService;

    @Operation(summary = "Listar faturas", description = "Lista as faturas ordenadas por data de vencimento decrescente, permitindo filtrar por cartão, competência (MM/yyyy) e status.")
    @GetMapping
    public List<FaturaResponseDTO> listar(
            @RequestParam(required = false) Long cartaoCreditoId,
            @RequestParam(required = false) String mesAno,
            @RequestParam(required = false) StatusFatura status) {
        return faturaService.listar(cartaoCreditoId, mesAno, status);
    }

    @Operation(summary = "Buscar fatura por ID", description = "Retorna os detalhes e valores consolidados de uma fatura específica.")
    @GetMapping("/{id}")
    public FaturaResponseDTO buscarPorId(@PathVariable Long id) {
        return faturaService.buscarPorId(id);
    }
}

