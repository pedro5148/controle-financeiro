package meufinanceiro_api.faturamento.mapper;

import meufinanceiro_api.faturamento.domain.Fatura;
import meufinanceiro_api.faturamento.dto.FaturaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FaturaMapper {

    @Mapping(target = "cartaoCreditoId", source = "cartao.id")
    @Mapping(target = "cartaoNome", source = "cartao.nome")
    @Mapping(target = "saldoRestante", expression = "java(entity.getSaldoRestante())")
    FaturaResponseDTO toResponseDTO(Fatura entity);
}

