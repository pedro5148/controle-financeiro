package meufinanceiro_api.financeiro.mapper;

import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.dto.CategoriaRequestDTO;
import meufinanceiro_api.financeiro.dto.CategoriaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subcategorias", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "versao", ignore = true)
    Categoria toEntity(CategoriaRequestDTO dto);

    CategoriaResponseDTO toResponseDTO(Categoria entity);

    @Named("semSubcategorias")
    @Mapping(target = "subcategorias", ignore = true)
    CategoriaResponseDTO toResponseDTOSemSubcategorias(Categoria entity);
}
