package meufinanceiro_api.financeiro.dto;

import meufinanceiro_api.financeiro.enums.TipoLancamento;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioPorCategoriaResponseDTO(
        Integer mes,
        Integer ano,
        TipoLancamento tipo,
        BigDecimal valorTotalPeriodo,
        Long quantidadeTotalLancamentos,
        List<RelatorioCategoriaItemDTO> categorias
) {
}

