package meufinanceiro_api.financeiro.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Trata nossos erros de domínio (ex: Conta sem saldo, categoria inválida) -> Retorna 400
    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardError handleRegraNegocio(RegraNegocioException ex) {
        return new StandardError(400, "Regra de Negócio", ex.getMessage(), null);
    }

    // Trata os orElseThrow quando um ID não existe no banco -> Retorna 404
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public StandardError handleEntityNotFound(EntityNotFoundException ex) {
        return new StandardError(404, "Não Encontrado", ex.getMessage(), null);
    }

    // Trata as anotações @NotNull e @NotBlank do seu LancamentoRequestDTO -> Retorna 400 com os campos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardError handleValidations(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                erros.put(error.getField(), error.getDefaultMessage())
        );
        return new StandardError(400, "Erro de Validação", "Verifique os campos enviados.", erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardError handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String mensagemAmigavel = "Corpo da requisição inválido ou mal formatado.";

        if (ex.getCause() instanceof InvalidFormatException ife && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            mensagemAmigavel = String.format("Valor inválido: '%s'. Os valores aceitos são: %s",
                    ife.getValue(), java.util.Arrays.toString(ife.getTargetType().getEnumConstants()));
        }

        return new StandardError(400, "Erro de Payload", mensagemAmigavel, null);
    }

    @ExceptionHandler(ConflitoNegocioException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardError handleConflitoNegocio(ConflitoNegocioException ex) {
        return new StandardError(409, "Conflito", ex.getMessage(), null);
    }

    @ExceptionHandler({
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.PessimisticLockingFailureException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardError handleLockingFailure(Exception ex) {
        return new StandardError(409, "Conflito de Concorrência", "Este registro foi alterado simultaneamente por outra transação. Recarregue os dados e tente novamente.", null);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public org.springframework.http.ResponseEntity<StandardError> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        String detalhe = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (detalhe != null && detalhe.toLowerCase().contains("uk_correcoes_faturas_chave_idempotencia")) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new StandardError(409, "Conflito", "Esta correção já foi processada anteriormente (chave de idempotência duplicada).", null));
        }
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new StandardError(400, "Violação de Integridade", "A operação não pode ser concluída pois o registro está vinculado a outros recursos do sistema.", null));
    }
}
