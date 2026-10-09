package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequestDTO(
        @NotBlank(message = "O nome da categoria é obrigatório")
        String nome,
        String subcategoriaNome,
        Boolean permiteDetalhamento
) {
}
