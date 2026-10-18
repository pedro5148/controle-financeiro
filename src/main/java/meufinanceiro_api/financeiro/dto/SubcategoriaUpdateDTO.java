package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubcategoriaUpdateDTO(
        @NotBlank(message = "O nome da subcategoria é obrigatório")
        @Size(min = 3, max = 100, message = "O nome da subcategoria deve ter entre 3 e 100 caracteres")
        String nome
) {
    public SubcategoriaUpdateDTO {
        nome = (nome != null) ? nome.trim() : null;
    }
}

