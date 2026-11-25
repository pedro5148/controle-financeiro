package meufinanceiro_api.financeiro.exception;

public class ConflitoNegocioException extends RuntimeException {
    public ConflitoNegocioException(String message) {
        super(message);
    }
}
