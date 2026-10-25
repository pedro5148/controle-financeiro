package meufinanceiro_api.financeiro.dto;

import java.math.BigDecimal;

public record ResumoPendenteTipoDTO(
        long quantidade,
        BigDecimal valorTotal
) {
}