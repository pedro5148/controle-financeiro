package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ContaRequestDTO(
        @NotBlank(message = "O nome da conta é obrigatório")
        @Size(min = 2, max = 100, message = "O nome da conta deve ter entre 2 e 100 caracteres")
        String nome,

        @PositiveOrZero(message = "O saldo inicial deve ser maior ou igual a zero")
        @Digits(integer = 15, fraction = 2, message = "O saldo inicial deve ter no máximo 15 dígitos inteiros e 2 decimais")
        BigDecimal saldoInicial,
        
        Boolean padrao,
        Boolean ativo
) {
}
