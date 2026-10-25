package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.service.strategy.LancamentoNormalStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LancamentoExclusaoTest {
    private final LancamentoRepository repository = mock(LancamentoRepository.class);
    private final LancamentoNormalStrategy strategy = new LancamentoNormalStrategy(repository, null, null, null, null);
    private final LancamentoService service = new LancamentoService(repository, null, null, List.of(strategy));

    private Lancamento parcela(long id, int numero, TipoLancamento tipo, StatusLancamento status, Conta conta, Conta destino) {
        Lancamento item = new Lancamento();
        item.setId(id); item.setGrupoParcelamentoId(10L); item.setParcelaAtual(numero);
        item.setTipo(tipo); item.setStatus(status);
        item.setValor(new BigDecimal("25")); item.setConta(conta); item.setContaDestino(destino);
        // As datas podem ser iguais ou editadas; a ordem deve vir da numeração da parcela.
        item.setDataLancamento(LocalDate.of(2026, 10, 1));
        return item;
    }

    @Test
    void excluiSelecionadaEPosterioresEstornandoReceitaDespesaETransferencia() {
        for (TipoLancamento tipo : TipoLancamento.values()) {
            reset(repository);
            Conta origem = new Conta(); origem.setSaldoAtual(new BigDecimal("100"));
            Conta destino = new Conta(); destino.setSaldoAtual(new BigDecimal("100"));
            Lancamento anterior = parcela(10, 1, tipo, StatusLancamento.PENDENTE, origem, destino);
            Lancamento selecionada = parcela(11, 2, tipo, StatusLancamento.EFETIVADO, origem, destino);
            Lancamento proxima = parcela(12, 3, tipo, StatusLancamento.PENDENTE, origem, destino);
            when(repository.findById(11L)).thenReturn(Optional.of(selecionada));
            when(repository.findByGrupoParcelamentoIdAndParcelaAtualGreaterThanEqualOrderByParcelaAtualAsc(10L, 2))
                    .thenReturn(List.of(selecionada, proxima));
            service.excluir(11L, false, true);
            verify(repository).delete(selecionada); verify(repository).delete(proxima);
            verify(repository, never()).delete(anterior);
            assertEquals(new BigDecimal(tipo == TipoLancamento.RECEITA ? "75" : "125"), origem.getSaldoAtual());
            assertEquals(new BigDecimal(tipo == TipoLancamento.TRANSFERENCIA ? "75" : "100"), destino.getSaldoAtual());
        }
    }

    @Test
    void exclusaoIndividualPreservaOutrasParcelas() {
        Conta conta = new Conta(); conta.setSaldoAtual(new BigDecimal("100"));
        Lancamento selecionada = parcela(11, 2, TipoLancamento.DESPESA, StatusLancamento.PENDENTE, conta, null);
        when(repository.findById(11L)).thenReturn(Optional.of(selecionada));
        service.excluir(11L, false, false);
        verify(repository).delete(selecionada);
        verify(repository, never()).findByGrupoParcelamentoIdAndParcelaAtualGreaterThanEqualOrderByParcelaAtualAsc(anyLong(), anyInt());
        assertEquals(new BigDecimal("100"), conta.getSaldoAtual());
    }

    @Test
    void rejeitaAlcancesConflitantesAvulsoEIdInexistente() {
        assertThrows(RegraNegocioException.class, () -> service.excluir(11L, true, true));
        verifyNoInteractions(repository);
        Lancamento avulso = new Lancamento();
        when(repository.findById(11L)).thenReturn(Optional.of(avulso));
        assertThrows(RegraNegocioException.class, () -> service.excluir(11L, false, true));
        when(repository.findById(12L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.excluir(12L, false, true));
        verify(repository, never()).delete(any(Lancamento.class));
    }
}
