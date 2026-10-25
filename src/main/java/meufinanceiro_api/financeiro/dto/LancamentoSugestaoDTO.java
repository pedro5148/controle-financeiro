package meufinanceiro_api.financeiro.dto;

public record LancamentoSugestaoDTO(
        String descricao,
        CategoriaResponseDTO categoria,
        SubcategoriaResponseDTO subcategoria
) {
}

