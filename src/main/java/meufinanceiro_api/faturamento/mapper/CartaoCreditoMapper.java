package meufinanceiro_api.faturamento.mapper;

import meufinanceiro_api.faturamento.domain.CartaoCredito;
import meufinanceiro_api.faturamento.dto.CartaoCreditoRequestDTO;
import meufinanceiro_api.faturamento.dto.CartaoCreditoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface CartaoCreditoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "principal", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "versao", ignore = true)
    CartaoCredito toEntity(CartaoCreditoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "principal", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "versao", ignore = true)
    void atualizarEntidade(CartaoCreditoRequestDTO dto, @MappingTarget CartaoCredito entity);

    @Mapping(target = "limiteUtilizado", source = "limiteUtilizado")
    @Mapping(target = "limiteDisponivel", source = "limiteDisponivel")
    CartaoCreditoResponseDTO toResponseDTO(CartaoCredito entity, BigDecimal limiteUtilizado, BigDecimal limiteDisponivel);
}

