package meufinanceiro_api.financeiro.dto;

public record CategoriaResponseDTO(
        Long id,
        String nome,
        Boolean permiteDetalhamento
) {
}
