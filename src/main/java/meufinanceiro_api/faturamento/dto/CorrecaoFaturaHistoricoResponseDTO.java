package meufinanceiro_api.faturamento.dto;

import meufinanceiro_api.faturamento.enums.StatusFatura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CorrecaoFaturaHistoricoResponseDTO(
        Long id,
        Long faturaId,
        String faturaMesAno,
        String chaveIdempotencia,
        Long lancamentoId,
        String descricaoDespesa,
        BigDecimal valorDespesa,
        LocalDate dataCompra,
        boolean pagamentoCorrigido,
        Long pagamentoFaturaId,
        Long contaAnteriorId,
        Long contaNovaId,
        LocalDate dataPagamentoAnterior,
        LocalDate dataPagamentoNova,
        BigDecimal valorPagamentoAnterior,
        BigDecimal valorPagamentoNovo,
        BigDecimal valorTotalAnterior,
        BigDecimal valorTotalNovo,
        BigDecimal valorPagoAnterior,
        BigDecimal valorPagoNovo,
        BigDecimal saldoRestanteAnterior,
        BigDecimal saldoRestanteNovo,
        StatusFatura statusAnterior,
        StatusFatura statusNovo,
        String motivo,
        LocalDateTime dataHoraCorrecao
) {
}
