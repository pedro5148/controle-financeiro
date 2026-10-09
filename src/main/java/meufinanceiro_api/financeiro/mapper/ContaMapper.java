package meufinanceiro_api.financeiro.mapper;

import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.dto.ContaRequestDTO;
import meufinanceiro_api.financeiro.dto.ContaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "saldoAtual",  ignore = true)
    Conta toEntity(ContaRequestDTO dto);
    ContaResponseDTO toResponseDTO(Conta entity);
}
