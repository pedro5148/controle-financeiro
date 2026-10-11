package meufinanceiro_api.financeiro.service.strategy;

import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.enums.MeioPagamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.enums.TipoRecorrencia;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.LancamentoMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.SubcategoriaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LancamentoNormalStrategy extends AbstractLancamentoStrategy {

    public LancamentoNormalStrategy(LancamentoRepository repository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository,
            SubcategoriaRepository subcategoriaRepository,
            LancamentoMapper mapper) {
        super(repository, contaRepository, categoriaRepository, subcategoriaRepository, mapper);
    }

    @Override
    public boolean isApplicable(LancamentoRequestDTO dto) {
        // Se não for crédito, é um lançamento normal (dinheiro, débito, pix,
        // transferência)
        return dto.meioPagamento() != MeioPagamento.CREDITO;
    }

    @Override
    public boolean isApplicable(Lancamento lancamento) {
        return lancamento.getMeioPagamento() != MeioPagamento.CREDITO;
    }

    @Override
    protected void preencherEntidadesEspecificas(Lancamento lancamento, LancamentoRequestDTO dto) {
        // Garante que não existem vínculos acidentais de cartão num lançamento normal
        lancamento.setFaturaId(null);
        lancamento.setCartaoCreditoId(null);
        lancamento.setLancamentoParcelado(null);
    }

    @Override
    protected List<Lancamento> gerarESalvarLancamentos(Lancamento lancamentoBase, LancamentoRequestDTO dto) {
        List<Lancamento> lancamentosSalvos = new ArrayList<>();

        if (lancamentoBase.getTipoRecorrencia() == TipoRecorrencia.MENSAL) {
            // A geração mensal cria 12 meses consecutivos
            int parcelas = 12;
            for (int i = 1; i <= parcelas; i++) {
                Lancamento p = copiarLancamento(lancamentoBase);
                p.setParcelaAtual(i);
                p.setStatus(i == 1 ? lancamentoBase.getStatus() : StatusLancamento.PENDENTE);

                if (i > 1) {
                    p.setDataLancamento(lancamentoBase.getDataLancamento().plusMonths(i - 1));
                    p.setDataVencimento(lancamentoBase.getDataVencimento().plusMonths(i - 1));
                    p.setDataEfetivacao(null);
                }
                lancamentosSalvos.add(repository.save(p));
            }
        } else {
            // Lançamento avulso (ÚNICO)
            lancamentoBase.setParcelaAtual(1);
            lancamentosSalvos.add(repository.save(lancamentoBase));
        }
        return lancamentosSalvos;
    }

    @Override
    protected void posProcessar(List<Lancamento> lancamentosSalvos) {
        for (Lancamento salvo : lancamentosSalvos) {
            if (salvo.getStatus() != StatusLancamento.EFETIVADO) {
                continue;
            }

            if (salvo.getConta() == null || salvo.getConta().getId() == null) {
                throw new RegraNegocioException(
                        "A conta de origem é obrigatória para despesas, receitas e transferências.");
            }

            Long contaOrigemId = salvo.getConta().getId();
            BigDecimal valor = salvo.getValor();

            if (salvo.getTipo() == TipoLancamento.RECEITA) {
                // conta.setSaldoAtual(saldoAtual.add(salvo.getValor()));
                contaRepository.creditarSaldo(contaOrigemId, valor);
            } else if (salvo.getTipo() == TipoLancamento.DESPESA) {
                // conta.setSaldoAtual(saldoAtual.subtract(salvo.getValor()));
                contaRepository.debitarSaldo(contaOrigemId, valor);
            } else if (salvo.getTipo() == TipoLancamento.TRANSFERENCIA) {
                if (salvo.getContaDestino() == null || salvo.getContaDestino().getId() == null) {
                    throw new RegraNegocioException("A conta de destino é obrigatória para transferências.");
                }
                // Grava a atualização do saldo no banco
                contaRepository.debitarSaldo(contaOrigemId, valor);
                contaRepository.creditarSaldo(salvo.getContaDestino().getId(), valor);
            }
            salvo.setConta(contaRepository.findById(contaOrigemId).orElseThrow());

            if (salvo.getTipo() == TipoLancamento.TRANSFERENCIA) {
                salvo.setContaDestino(contaRepository.findById(salvo.getContaDestino().getId()).orElseThrow());
            }
        }
    }

    @Override
    protected void reverterImpactoFinanceiro(Lancamento lancamentoAntigo) {
        // Se o lançamento não estava efetivado no passado, ele nunca alterou o saldo.
        if (lancamentoAntigo.getStatus() != StatusLancamento.EFETIVADO) {
            return;
        }

        BigDecimal valorAntigo = lancamentoAntigo.getValor();
        Long contaId = lancamentoAntigo.getConta().getId();

        // Operação INVERSA ao que foi feito na criação
        switch (lancamentoAntigo.getTipo()) {
            case DESPESA -> contaRepository.creditarSaldo(contaId, valorAntigo);
            case RECEITA -> contaRepository.debitarSaldo(contaId, valorAntigo);
            case TRANSFERENCIA -> {
                contaRepository.creditarSaldo(contaId, valorAntigo); // Devolve para a origem
                contaRepository.debitarSaldo(lancamentoAntigo.getContaDestino().getId(), valorAntigo); // Tira do
                                                                                                       // destino
            }
        }
    }

    @Override
    protected Lancamento atualizarESalvarLancamento(Lancamento lancamento) {

        Lancamento salvo = repository.save(lancamento);
        repository.flush();

        // Aplica a atualizacao financeira (identico à lógica de criação)
        if (salvo.getStatus() == StatusLancamento.EFETIVADO) {
            BigDecimal novoValor = salvo.getValor();
            Long novaContaId = salvo.getConta().getId();

            switch (salvo.getTipo()) {
                case DESPESA -> contaRepository.debitarSaldo(novaContaId, novoValor);
                case RECEITA -> contaRepository.creditarSaldo(novaContaId, novoValor);
                case TRANSFERENCIA -> {
                    contaRepository.debitarSaldo(novaContaId, novoValor); // Tira da origem
                    contaRepository.creditarSaldo(salvo.getContaDestino().getId(), novoValor); // Coloca no destino
                }
            }
            salvo.setConta(contaRepository.findById(novaContaId).orElseThrow());

            if (salvo.getTipo() == TipoLancamento.TRANSFERENCIA && salvo.getContaDestino() != null) {
                salvo.setContaDestino(contaRepository.findById(salvo.getContaDestino().getId()).orElseThrow());
            }
        }
        return salvo;
    }

    @Override
    protected void posProcessarAtualizacao(Lancamento salvo, LancamentoRequestDTO dto) {
    }

    @Override
    protected void posProcessarExclusao(Lancamento lancamento) {
    }
}