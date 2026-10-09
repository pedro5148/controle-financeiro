package meufinanceiro_api.financeiro.mapper;

import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.dto.CategoriaRequestDTO;
import meufinanceiro_api.financeiro.dto.CategoriaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {
    @Mapping(target = "id", ignore = true)
    //@Mapping(target = "permiteDetalhamento", ignore = true)
    Categoria toEntity(CategoriaRequestDTO dto);
    CategoriaResponseDTO toResponseDTO(Categoria entity);
}
