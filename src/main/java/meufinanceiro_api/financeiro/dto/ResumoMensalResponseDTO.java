package meufinanceiro_api.financeiro.dto;

public record ResumoMensalResponseDTO(
        Integer mes,
        Integer ano,
        ResumoPendenteTipoDTO despesas,
        ResumoPendenteTipoDTO receitas,
        ResumoPendenteTipoDTO transferencias
) {
}