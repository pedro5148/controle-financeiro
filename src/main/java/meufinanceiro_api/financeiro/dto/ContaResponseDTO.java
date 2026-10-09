package meufinanceiro_api.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaResponseDTO(
        Long id,
        String nome,
        BigDecimal saldoInicial,
        BigDecimal saldoAtual,
        LocalDate dataCriacao,
        boolean padrao
) {
}
