package meufinanceiro_api.financeiro.service.strategy;

import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
import meufinanceiro_api.financeiro.dto.LancamentoUpdateDTO;

import java.util.List;

public interface LancamentoStrategy {
    boolean isApplicable(LancamentoRequestDTO dto);
    boolean isApplicable(LancamentoUpdateDTO dto);
    boolean isApplicable(Lancamento lancamento);
    List<LancamentoResponseDTO> processar(LancamentoRequestDTO dto);
    LancamentoResponseDTO atualizar(Lancamento existente, LancamentoUpdateDTO dto);
    void excluir(Lancamento lancamento, boolean excluirPendentesDoGrupo);
}
