package meufinanceiro_api.faturamento.dto;

import meufinanceiro_api.faturamento.enums.StatusFatura;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FaturaResponseDTO(
        Long id,
        Long cartaoCreditoId,
        String cartaoNome,
        String mesAno,
        BigDecimal valorTotal,
        BigDecimal valorPago,
        BigDecimal saldoRestante,
        StatusFatura status,
        LocalDate dataFechamento,
        LocalDate dataVencimento
) {
}

