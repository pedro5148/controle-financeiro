package meufinanceiro_api.financeiro.mapper;

import meufinanceiro_api.financeiro.domain.Subcategoria;
import meufinanceiro_api.financeiro.dto.SubcategoriaRequestDTO;
import meufinanceiro_api.financeiro.dto.SubcategoriaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CategoriaMapper.class})
public interface SubcategoriaMapper {
    @Mapping(target = "categoria.id", source = "categoriaId")
    @Mapping(target = "id", ignore = true)
    Subcategoria toEntity(SubcategoriaRequestDTO dto);

    SubcategoriaResponseDTO toResponseDTO(Subcategoria entity);
}
