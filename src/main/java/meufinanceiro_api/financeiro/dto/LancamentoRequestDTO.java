package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import meufinanceiro_api.financeiro.enums.MeioPagamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.enums.TipoRecorrencia;
import meufinanceiro_api.financeiro.enums.TipoValorParcelamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRequestDTO(

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

        // Sem @NotNull, pois compras a crédito não abatem saldo da conta de imediato
        Long contaId,
        Long contaDestinoId,

        @NotNull(message = "A categoria é obrigatória")
        Long categoriaId,
        Long subcategoriaId,

        @NotNull(message = "A data de lançamento é obrigatória")
        LocalDate dataLancamento,
        @NotNull(message = "A data de vencimento é obrigatória")
        LocalDate dataVencimento,
        LocalDate dataEfetivacao,

        @NotNull(message = "O status é obrigatório")
        StatusLancamento status,

        String observacoes,

        @NotNull(message = "O tipo de recorrência é obrigatório")
        TipoRecorrencia tipoRecorrencia,

        // Gestão de Parcelas - Apenas o necessário para a entrada
        Integer quantidadeParcelas,
        TipoValorParcelamento tipoValorParcelamento,

        // Soft Links - Faturamento e Fiscal
        Long cartaoCreditoId,
        Long documentoFiscalId
) {
        public LancamentoRequestDTO {
                descricao = (descricao != null) ? descricao.trim() : null;
                observacoes = (observacoes == null || observacoes.isBlank())
                        ? null
                        : observacoes.trim();
        }
}