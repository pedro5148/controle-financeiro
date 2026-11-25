package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EfetivacaoLancamentoRequestDTO(
        @Positive(message = "O valor deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "O valor deve ter no máximo 17 dígitos inteiros e 2 decimais")
        BigDecimal valor,

        Long contaId,
        Long contaDestinoId,

        LocalDate dataEfetivacao
) {
}
