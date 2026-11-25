package meufinanceiro_api.faturamento.mapper;

import meufinanceiro_api.faturamento.domain.CorrecaoFatura;
import meufinanceiro_api.faturamento.dto.CorrecaoFaturaHistoricoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CorrecaoFaturaMapper {

    @Mapping(target = "faturaId", source = "fatura.id")
    @Mapping(target = "faturaMesAno", source = "fatura.mesAno")
    CorrecaoFaturaHistoricoResponseDTO toHistoricoResponseDTO(CorrecaoFatura entity);
}
