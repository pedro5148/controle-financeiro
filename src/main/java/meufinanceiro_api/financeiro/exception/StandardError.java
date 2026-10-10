package meufinanceiro_api.financeiro.exception;

import java.util.Map;

public record StandardError(
        int status,
        String titulo,
        String detalhe,
        Map<String, String> validacoes
) {
}
