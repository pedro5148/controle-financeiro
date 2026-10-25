package meufinanceiro_api.financeiro.dto;

import java.math.BigDecimal;

public record RelatorioSubcategoriaItemDTO(
        Long subcategoriaId,
        String subcategoriaNome,
        BigDecimal valorTotal,
        Long quantidadeLancamentos,
        BigDecimal percentualDaCategoria,
        BigDecimal percentualDoTotal
) {
}

