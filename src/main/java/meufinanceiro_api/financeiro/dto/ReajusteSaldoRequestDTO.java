package meufinanceiro_api.financeiro.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import meufinanceiro_api.financeiro.enums.MeioPagamento;

import java.math.BigDecimal;

public record ReajusteSaldoRequestDTO(
        @NotNull(message = "O novo saldo atual é obrigatório")
        @Digits(integer = 15, fraction = 2, message = "O novo saldo atual deve ter no máximo 15 dígitos inteiros e 2 decimais")
        BigDecimal novoSaldoAtual,

        Long categoriaId,

        Long subcategoriaId,

        MeioPagamento meioPagamento,

        @Size(max = 100, message = "A descrição deve ter no máximo 100 caracteres")
        String descricao,

        String observacoes
) {
}

