package meufinanceiro_api.financeiro.dto;

import java.math.BigDecimal;

public record CategoriaSubcategoriaResumoProjection(
        Long categoriaId,
        String categoriaNome,
        Long subcategoriaId,
        String subcategoriaNome,
        BigDecimal valorTotal,
        Long quantidadeLancamentos
) {
}

