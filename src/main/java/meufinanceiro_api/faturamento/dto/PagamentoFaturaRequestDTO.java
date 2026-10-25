package meufinanceiro_api.faturamento.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagamentoFaturaRequestDTO(
        @Positive(message = "O valor do pagamento deve ser maior que zero")
        BigDecimal valor,

        Long contaId,

        LocalDate dataPagamento,

        @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres")
        String observacoes
) {
}
