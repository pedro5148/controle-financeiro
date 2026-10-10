package meufinanceiro_api.financeiro.mapper;

import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
//import org.springframework.web.bind.annotation.Mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {
        ContaMapper.class,
        CategoriaMapper.class,
        SubcategoriaMapper.class
})
public interface LancamentoMapper {

    @Mapping(target = "conta.id", source = "contaId")
    @Mapping(target = "contaDestino.id", source = "contaDestinoId")
    @Mapping(target = "categoria.id", source = "categoriaId")
    @Mapping(target = "subcategoria.id", source = "subcategoriaId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lancamentoParcelado", ignore = true)
    Lancamento toEntity(LancamentoRequestDTO dto);

    @Mapping(target = "parcelamento", source = "lancamentoParcelado")
    LancamentoResponseDTO toResponseDTO(Lancamento entity);

    @Mapping(target = "id", ignore = true) // Protege o ID contra alterações acidentais
    void atualizarEntidade(LancamentoRequestDTO dto, @MappingTarget Lancamento entidade);
}
