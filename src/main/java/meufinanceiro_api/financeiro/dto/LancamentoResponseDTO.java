package meufinanceiro_api.financeiro.dto;

import meufinanceiro_api.financeiro.enums.MeioPagamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.enums.TipoRecorrencia;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoResponseDTO(
        Long id,
        TipoLancamento tipo,
        MeioPagamento meioPagamento,
        String descricao,
        BigDecimal valor,

        ContaResponseDTO conta,
        ContaResponseDTO contaDestino,
        CategoriaResponseDTO categoria,
        SubcategoriaResponseDTO subcategoria,

        LocalDate dataLancamento,
        LocalDate dataVencimento,
        LocalDate dataEfetivacao,

        StatusLancamento status,
        String observacoes,

        // Dados de Recorrência e Parcelamento
        TipoRecorrencia tipoRecorrencia,
        Integer parcelaAtual,
        Integer totalParcelas,
        Long grupoParcelamentoId,

        // Soft Links com Módulo de Faturamento
        Long cartaoCreditoId,
        Long faturaId,

        // Soft Link com Módulo Fiscal
        Long documentoFiscalId
) {
}
