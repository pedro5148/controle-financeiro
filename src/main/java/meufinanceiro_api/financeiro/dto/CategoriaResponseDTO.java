package meufinanceiro_api.financeiro.dto;

import java.util.List;

public record CategoriaResponseDTO(
        Long id,
        String nome,
        List<SubcategoriaResponseDTO> subcategorias
) {
}
