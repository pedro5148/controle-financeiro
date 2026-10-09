package meufinanceiro_api.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoParceladoResponseDTO(
        Long id,
        String descricao,
        Integer quantidadeParcelas,
        BigDecimal valorTotal,
        LocalDate dataInicio
) {
}
