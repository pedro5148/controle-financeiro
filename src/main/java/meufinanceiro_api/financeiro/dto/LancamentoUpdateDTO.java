package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import meufinanceiro_api.financeiro.enums.MeioPagamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoUpdateDTO(

        @NotNull(message = "O tipo de lançamento é obrigatório")
        TipoLancamento tipo,
        @NotNull(message = "O meio de pagamento é obrigatório")
        MeioPagamento meioPagamento,
        @NotBlank(message = "A descrição é obrigatória")
        @Size(min = 2, max = 100, message = "A descrição deve ter entre 2 e 100 caracteres")
        String descricao,
        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "O valor deve ter no máximo 17 dígitos inteiros e 2 decimais")
        BigDecimal valor,

        Long contaId,
        Long contaDestinoId,

        @NotNull(message = "A categoria é obrigatória")
        Long categoriaId,
        Long subcategoriaId,

        @NotNull(message = "A data de lançamento é obrigatória")
        LocalDate dataLancamento,
        // Obrigatório apenas para lançamentos normais; no cartão de crédito assume o vencimento da fatura
        LocalDate dataVencimento,
        LocalDate dataEfetivacao,

        // Obrigatório apenas para lançamentos normais; no cartão de crédito permanece PENDENTE até o pagamento da fatura
        StatusLancamento status,

        String observacoes,

        // Soft Links - Faturamento e Fiscal
        Long cartaoCreditoId,
        Long documentoFiscalId
) {
        public LancamentoUpdateDTO {
                descricao = (descricao != null) ? descricao.trim() : null;
                observacoes = (observacoes == null || observacoes.isBlank())
                        ? null
                        : observacoes.trim();
        }
}

