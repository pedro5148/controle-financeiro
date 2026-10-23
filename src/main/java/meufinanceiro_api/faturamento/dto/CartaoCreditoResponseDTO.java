package meufinanceiro_api.faturamento.dto;

import java.math.BigDecimal;

public record CartaoCreditoResponseDTO(
        Long id,
        String nome,
        BigDecimal limiteTotal,
        BigDecimal limiteUtilizado,
        BigDecimal limiteDisponivel,
        Integer diaFechamento,
        Integer diaVencimento,
        Long contaPadraoId,
        String bandeira,
        boolean principal,
        boolean ativo
) {
}

