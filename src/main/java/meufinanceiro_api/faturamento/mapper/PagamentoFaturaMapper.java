package meufinanceiro_api.faturamento.mapper;

import meufinanceiro_api.faturamento.domain.PagamentoFatura;
import meufinanceiro_api.faturamento.dto.PagamentoFaturaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PagamentoFaturaMapper {

    @Mapping(target = "faturaId", source = "fatura.id")
    @Mapping(target = "faturaMesAno", source = "fatura.mesAno")
    @Mapping(target = "contaId", source = "conta.id")
    @Mapping(target = "contaNome", source = "conta.nome")
    PagamentoFaturaResponseDTO toResponseDTO(PagamentoFatura entity);
}
