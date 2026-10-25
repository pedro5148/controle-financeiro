package meufinanceiro_api.financeiro.dto;

import java.math.BigDecimal;

public record SaldoMensalResponseDTO(
        BigDecimal saldoInicialMes,
        BigDecimal saldoAtual,
        BigDecimal saldoPrevistoFimMes
) {
}

