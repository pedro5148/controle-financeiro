package meufinanceiro_api.financeiro.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoriaResponseDTO(
        Long id,
        String nome,
        boolean ativo,
        List<SubcategoriaResponseDTO> subcategorias
) {
}
