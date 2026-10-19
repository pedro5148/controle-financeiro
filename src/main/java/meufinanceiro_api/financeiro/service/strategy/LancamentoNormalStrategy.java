package meufinanceiro_api.financeiro.service.strategy;

import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.enums.MeioPagamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.enums.TipoRecorrencia;
import meufinanceiro_api.financeiro.enums.TipoValorParcelamento;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.LancamentoMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.SubcategoriaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        // transferência, boleto)
        return dto.meioPagamento() != MeioPagamento.CREDITO;
    }

    @Override
    public boolean isApplicable(Lancamento lancamento) {
        return lancamento.getMeioPagamento() != MeioPagamento.CREDITO;
    }

    @Override
    protected void preencherEntidadesEspecificas(Lancamento lancamento, LancamentoRequestDTO dto) {
        if (lancamento.getConta() == null || lancamento.getConta().getId() == null) {
            throw new RegraNegocioException(
                    "A conta de origem é obrigatória para lançamentos que não sejam no cartão de crédito.");
        }

        if (dto.cartaoCreditoId() != null) {
            throw new RegraNegocioException(
                    "O cartão de crédito só deve ser informado quando o meio de pagamento for CREDITO.");
        }

        lancamento.setFaturaId(null);
        lancamento.setCartaoCreditoId(null);
    }

    @Override
    protected List<Lancamento> gerarESalvarLancamentos(Lancamento lancamentoBase, LancamentoRequestDTO dto) {
        List<Lancamento> lancamentosSalvos = new ArrayList<>();

        if (lancamentoBase.getTipoRecorrencia() == TipoRecorrencia.PARCELADO) {
            int parcelas = dto.quantidadeParcelas();
            BigDecimal quantidadeBD = BigDecimal.valueOf(parcelas);
            BigDecimal valorInformado = dto.valor();

            BigDecimal valorParcelaBase;
            BigDecimal restoCentavos = BigDecimal.ZERO;

            if (dto.tipoValorParcelamento() == TipoValorParcelamento.VALOR_PARCELA) {
                valorParcelaBase = valorInformado;
            } else {
                // VALOR_TOTAL: divide pelas parcelas e aloca eventual diferença de centavos na 1ª parcela
                valorParcelaBase = valorInformado.divide(quantidadeBD, 2, RoundingMode.DOWN);
                restoCentavos = valorInformado.subtract(valorParcelaBase.multiply(quantidadeBD));
            }

            Long grupoId = null;

            for (int i = 1; i <= parcelas; i++) {
                Lancamento p = copiarLancamento(lancamentoBase);
                p.setParcelaAtual(i);
                p.setTotalParcelas(parcelas);
                p.setGrupoParcelamentoId(grupoId);
                p.setValor(i == 1 ? valorParcelaBase.add(restoCentavos) : valorParcelaBase);
                p.setStatus(i == 1 ? lancamentoBase.getStatus() : StatusLancamento.PENDENTE);

                if (i > 1) {
                    p.setDataLancamento(lancamentoBase.getDataLancamento().plusMonths(i - 1));
                    p.setDataVencimento(lancamentoBase.getDataVencimento().plusMonths(i - 1));
                    p.setDataEfetivacao(null);
                }

                Lancamento salvo = repository.save(p);
                if (i == 1) {
                    grupoId = salvo.getId();
                    salvo.setGrupoParcelamentoId(grupoId);
                }
                lancamentosSalvos.add(salvo);
            }
        } else {
            // Lançamento avulso (NENHUMA)
            lancamentoBase.setParcelaAtual(1);
            lancamentoBase.setTotalParcelas(1);
            lancamentoBase.setGrupoParcelamentoId(null);
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

            BigDecimal valor = salvo.getValor();

            if (salvo.getTipo() == TipoLancamento.RECEITA) {
                salvo.getConta().creditar(valor);
            } else if (salvo.getTipo() == TipoLancamento.DESPESA) {
                salvo.getConta().debitar(valor);
            } else if (salvo.getTipo() == TipoLancamento.TRANSFERENCIA) {
                if (salvo.getContaDestino() == null || salvo.getContaDestino().getId() == null) {
                    throw new RegraNegocioException("A conta de destino é obrigatória para transferências.");
                }
                salvo.getConta().debitar(valor);
                salvo.getContaDestino().creditar(valor);
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

        // Operação INVERSA ao que foi feito na criação
        switch (lancamentoAntigo.getTipo()) {
            case DESPESA -> lancamentoAntigo.getConta().creditar(valorAntigo);
            case RECEITA -> lancamentoAntigo.getConta().debitar(valorAntigo);
            case TRANSFERENCIA -> {
                lancamentoAntigo.getConta().creditar(valorAntigo); // Devolve para a origem
                lancamentoAntigo.getContaDestino().debitar(valorAntigo); // Tira do destino
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

            switch (salvo.getTipo()) {
                case DESPESA -> salvo.getConta().debitar(novoValor);
                case RECEITA -> salvo.getConta().creditar(novoValor);
                case TRANSFERENCIA -> {
                    salvo.getConta().debitar(novoValor); // Tira da origem
                    salvo.getContaDestino().creditar(novoValor); // Coloca no destino
                }
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