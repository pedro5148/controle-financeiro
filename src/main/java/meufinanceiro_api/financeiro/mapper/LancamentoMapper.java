package meufinanceiro_api.financeiro.mapper;

import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {
        ContaMapper.class,
        CategoriaMapper.class,
        SubcategoriaMapper.class
})
public interface LancamentoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conta", ignore = true)
    @Mapping(target = "contaDestino", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "subcategoria", ignore = true)
    @Mapping(target = "parcelaAtual", ignore = true)
    @Mapping(target = "totalParcelas", ignore = true)
    @Mapping(target = "grupoParcelamentoId", ignore = true)
    @Mapping(target = "faturaId", ignore = true)
    Lancamento toEntity(LancamentoRequestDTO dto);

    @Mapping(target = "categoria", qualifiedByName = "semSubcategorias")
    LancamentoResponseDTO toResponseDTO(Lancamento entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conta", ignore = true)
    @Mapping(target = "contaDestino", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "subcategoria", ignore = true)
    @Mapping(target = "parcelaAtual", ignore = true)
    @Mapping(target = "totalParcelas", ignore = true)
    @Mapping(target = "grupoParcelamentoId", ignore = true)
    @Mapping(target = "faturaId", ignore = true)
    void atualizarEntidade(LancamentoRequestDTO dto, @MappingTarget Lancamento entidade);
}
