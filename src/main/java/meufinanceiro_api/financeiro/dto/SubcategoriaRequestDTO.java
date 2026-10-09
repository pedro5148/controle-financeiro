package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubcategoriaRequestDTO(
        @NotBlank(message = "O nome da subcategoria é obrigatório") String nome,
        @NotNull(message = "A categoria é obrigatória") Long categoriaId
) {
}
