package meufinanceiro_api.faturamento.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CartaoCreditoRequestDTO(
        @NotBlank(message = "O nome do cartão é obrigatório")
        @Size(min = 2, max = 100, message = "O nome do cartão deve ter entre 2 e 100 caracteres")
        String nome,

        @NotNull(message = "O limite total do cartão é obrigatório")
        @Positive(message = "O limite total deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "O limite total deve ter no máximo 17 dígitos inteiros e 2 decimais")
        BigDecimal limiteTotal,

        @NotNull(message = "O dia de fechamento é obrigatório")
        @Min(value = 1, message = "O dia de fechamento deve estar entre 1 e 31")
        @Max(value = 31, message = "O dia de fechamento deve estar entre 1 e 31")
        Integer diaFechamento,

        @NotNull(message = "O dia de vencimento é obrigatório")
        @Min(value = 1, message = "O dia de vencimento deve estar entre 1 e 31")
        @Max(value = 31, message = "O dia de vencimento deve estar entre 1 e 31")
        Integer diaVencimento,

        @NotNull(message = "A conta padrão para pagamento da fatura é obrigatória")
        Long contaPadraoId,

        @Size(max = 50, message = "A bandeira deve ter no máximo 50 caracteres")
        String bandeira,

        Boolean principal,
        Boolean ativo
) {
    public CartaoCreditoRequestDTO {
        nome = (nome != null) ? nome.trim() : null;
        bandeira = (bandeira == null || bandeira.isBlank()) ? null : bandeira.trim();
    }
}

