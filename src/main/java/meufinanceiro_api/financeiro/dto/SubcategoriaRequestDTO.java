package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubcategoriaRequestDTO(
        @NotBlank(message = "O nome da subcategoria é obrigatório") 
        @Size(min = 3, max = 100, message = "O nome da subcategoria deve ter entre 3 e 100 caracteres")
        String nome,
        @NotNull(message = "A categoria é obrigatória") Long categoriaId
) {
    public SubcategoriaRequestDTO {
        nome = (nome != null) ? nome.trim() : null;
    }
}
