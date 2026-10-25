package meufinanceiro_api.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioCategoriaItemDTO(
        Long categoriaId,
        String categoriaNome,
        BigDecimal valorTotal,
        Long quantidadeLancamentos,
        BigDecimal percentual,
        BigDecimal valorSemSubcategoria,
        List<RelatorioSubcategoriaItemDTO> subcategorias
) {
}

