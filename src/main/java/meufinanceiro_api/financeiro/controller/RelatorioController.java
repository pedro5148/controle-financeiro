package meufinanceiro_api.financeiro.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.dto.RelatorioPorCategoriaResponseDTO;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.service.RelatorioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Relatórios", description = "Endpoints analíticos e relatórios gerenciais")
@RestController
@RequestMapping("/api/relatorios")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @Operation(
            summary = "Relatório de lançamentos por categoria e subcategoria",
            description = "Retorna o agrupamento hierárquico de despesas (ou receitas) por categoria e subcategoria no período selecionado, com totais, quantidades e percentuais calculados."
    )
    @GetMapping("/por-categoria")
    public ResponseEntity<RelatorioPorCategoriaResponseDTO> porCategoria(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false, defaultValue = "DESPESA") TipoLancamento tipo,
            @RequestParam(required = false) StatusLancamento status,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) Long cartaoCreditoId) {
        return ResponseEntity.ok(
                relatorioService.gerarRelatorioPorCategoria(mes, ano, tipo, status, contaId, cartaoCreditoId)
        );
    }
}
