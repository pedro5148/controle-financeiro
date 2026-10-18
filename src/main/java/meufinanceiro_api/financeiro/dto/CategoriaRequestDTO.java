package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequestDTO(
        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(min = 3, max = 100, message = "O nome da categoria deve ter entre 3 e 100 caracteres")
        String nome,

        @Size(min = 3, max = 100, message = "O nome da Sub Categoria deve ter entre 3 e 100 caracteres")
        String subcategoriaNome
) {
    public CategoriaRequestDTO {
        nome = (nome != null) ? nome.trim() : null;
        subcategoriaNome = (subcategoriaNome == null || subcategoriaNome.isBlank())
                ? null
                : subcategoriaNome.trim();
    }
}
