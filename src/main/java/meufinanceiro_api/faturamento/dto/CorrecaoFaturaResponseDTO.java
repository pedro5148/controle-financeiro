package meufinanceiro_api.faturamento.dto;

import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;

import java.math.BigDecimal;

public record CorrecaoFaturaResponseDTO(
        CorrecaoFaturaHistoricoResponseDTO correcao,
        FaturaResponseDTO fatura,
        LancamentoResponseDTO lancamentoCriado,
        PagamentoFaturaResponseDTO pagamentoCorrigido,
        BigDecimal limiteUtilizadoCartao,
        BigDecimal limiteDisponivelCartao
) {
}
