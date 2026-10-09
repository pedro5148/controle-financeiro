package meufinanceiro_api.financeiro.service.strategy;

import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;

import java.util.List;

public interface LancamentoStrategy {
    boolean isApplicable(LancamentoRequestDTO dto);
    // Usado pelo DELETE
    boolean isApplicable(Lancamento lancamento);
    List<LancamentoResponseDTO> processar(LancamentoRequestDTO dto);
    LancamentoResponseDTO atualizar(Lancamento existente, LancamentoRequestDTO dto);
    void excluir(Lancamento lancamento);
}
