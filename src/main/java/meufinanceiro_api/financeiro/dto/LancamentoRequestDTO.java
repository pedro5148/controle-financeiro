package meufinanceiro_api.financeiro.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import meufinanceiro_api.financeiro.enums.MeioPagamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.enums.TipoRecorrencia;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRequestDTO(

        @NotNull(message = "O tipo de lançamento é obrigatório")
        TipoLancamento tipo,
        @NotNull(message = "O meio de pagamento é obrigatório")
        MeioPagamento meioPagamento,
        @NotBlank(message = "A descrição é obrigatória")
        String descricao,
        @NotNull(message = "O valor é obrigatório")
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

        // Soft Links - Faturamento e Fiscal
        Long cartaoCreditoId,
        Long faturaId,
        Long documentoFiscalId
) {
        @JsonIgnore
        @AssertTrue(message = "A quantidade de parcelas deve ser maior que 1 para lançamentos PARCELADOS e nula para os demais.")
        public boolean isParcelamentoValido() {
                if (tipoRecorrencia == TipoRecorrencia.PARCELADO) {
                        return quantidadeParcelas != null && quantidadeParcelas > 1;
                }
                // Se for NENHUMA ou MENSAL, o campo deve vir vazio para evitar lixo de estado
                return quantidadeParcelas == null;
        }
}
