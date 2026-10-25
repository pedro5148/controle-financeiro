package meufinanceiro_api.faturamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagamentoFaturaResponseDTO(
        Long id,
        Long faturaId,
        String faturaMesAno,
        Long contaId,
        String contaNome,
        BigDecimal valor,
        LocalDate dataPagamento,
        String observacoes
) {
}
