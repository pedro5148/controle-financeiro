package meufinanceiro_api.financeiro.mapper;

import meufinanceiro_api.financeiro.domain.Subcategoria;
import meufinanceiro_api.financeiro.dto.SubcategoriaResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubcategoriaMapper {
    SubcategoriaResponseDTO toResponseDTO(Subcategoria entity);
}
