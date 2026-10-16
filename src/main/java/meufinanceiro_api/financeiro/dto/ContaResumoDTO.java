package meufinanceiro_api.financeiro.dto;

import java.math.BigDecimal;

public record ContaResumoDTO(
        BigDecimal saldoTotal,
        long totalContas,
        long totalContasAtivas
) {
}
