package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ContaRequestDTO(
        @NotBlank(message = "O nome da conta é obrigatório")
        String nome,
        BigDecimal saldoInicial,
        boolean padrao
) {
}
