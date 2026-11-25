package meufinanceiro_api.faturamento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CorrigirFaturaPagaRequestDTO(
        @Size(max = 100, message = "A chave de idempotência deve ter no máximo 100 caracteres")
        String chaveIdempotencia,

        Long versaoFatura,

        @NotNull(message = "Os dados da despesa esquecida são obrigatórios")
        @Valid
        DespesaEsquecidaDTO despesa,

        @Valid
        CorrecaoPagamentoDTO correcaoPagamento,

        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres")
        String motivo
) {

    public record DespesaEsquecidaDTO(
            @NotBlank(message = "A descrição da despesa é obrigatória")
            @Size(max = 100, message = "A descrição deve ter no máximo 100 caracteres")
            String descricao,

            @NotNull(message = "O valor da despesa é obrigatório")
            @Positive(message = "O valor da despesa deve ser maior que zero")
            @Digits(integer = 17, fraction = 2, message = "O valor da despesa deve ter no máximo 17 dígitos inteiros e 2 casas decimais")
            BigDecimal valor,

            @NotNull(message = "A data original da compra é obrigatória")
            LocalDate dataLancamento,

            @NotNull(message = "A categoria é obrigatória")
            Long categoriaId,

            Long subcategoriaId,

            @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres")
            String observacoes,

            Long documentoFiscalId
    ) {
    }

    public record CorrecaoPagamentoDTO(
            @NotNull(message = "O ID do pagamento existente é obrigatório para corrigir o pagamento")
            Long pagamentoFaturaId,

            Long contaId,

            LocalDate dataPagamento,

            @Positive(message = "O novo valor do pagamento deve ser maior que zero")
            @Digits(integer = 17, fraction = 2, message = "O novo valor do pagamento deve ter no máximo 17 dígitos inteiros e 2 casas decimais")
            BigDecimal novoValor,

            @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres")
            String observacoes
    ) {
    }
}
