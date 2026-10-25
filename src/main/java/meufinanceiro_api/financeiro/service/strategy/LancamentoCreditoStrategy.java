package meufinanceiro_api.financeiro.service.strategy;

import meufinanceiro_api.faturamento.domain.CartaoCredito;
import meufinanceiro_api.faturamento.domain.Fatura;
import meufinanceiro_api.faturamento.service.FaturaService;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoUpdateDTO;
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
public class LancamentoCreditoStrategy extends AbstractLancamentoStrategy {

    private final FaturaService faturaService;

    public LancamentoCreditoStrategy(
            LancamentoRepository repository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository,
            SubcategoriaRepository subcategoriaRepository,
            LancamentoMapper mapper,
            FaturaService faturaService) {
        super(repository, contaRepository, categoriaRepository, subcategoriaRepository, mapper);
        this.faturaService = faturaService;
    }

    @Override
    public boolean isApplicable(LancamentoRequestDTO dto) {
        return dto.meioPagamento() == MeioPagamento.CREDITO;
    }

    @Override
    public boolean isApplicable(LancamentoUpdateDTO dto) {
        return dto.meioPagamento() == MeioPagamento.CREDITO;
    }

    @Override
    public boolean isApplicable(Lancamento lancamento) {
        return lancamento.getMeioPagamento() == MeioPagamento.CREDITO;
    }

    @Override
    protected void preencherEntidadesEspecificas(Lancamento lancamento, LancamentoRequestDTO dto) {
        validarEPreencherRegrasCredito(lancamento, dto.cartaoCreditoId());
    }

    @Override
    protected void preencherEntidadesEspecificasAtualizacao(Lancamento lancamento, LancamentoUpdateDTO dto) {
        validarEPreencherRegrasCredito(lancamento, dto.cartaoCreditoId());

        CartaoCredito cartao = faturaService.obterCartaoAtivo(lancamento.getCartaoCreditoId());
        faturaService.validarLimiteDisponivel(cartao, lancamento.getValor());

        int offsetMeses = (lancamento.getParcelaAtual() != null && lancamento.getParcelaAtual() > 1)
                ? lancamento.getParcelaAtual() - 1
                : 0;

        Fatura faturaDestino = faturaService.obterOuCriarFaturaAbertaParaCompra(
                cartao,
                lancamento.getDataLancamento(),
                offsetMeses
        );

        lancamento.setFaturaId(faturaDestino.getId());
        lancamento.setDataVencimento(faturaDestino.getDataVencimento());
    }

    private void validarEPreencherRegrasCredito(Lancamento lancamento, Long cartaoCreditoId) {
        if (lancamento.getTipo() != TipoLancamento.DESPESA) {
            throw new RegraNegocioException(
                    "Lançamentos no cartão de crédito devem ser exclusivamente do tipo DESPESA.");
        }

        if (lancamento.getConta() != null || lancamento.getContaDestino() != null) {
            throw new RegraNegocioException(
                    "Lançamentos no cartão de crédito não devem informar conta bancária de origem ou destino.");
        }

        if (cartaoCreditoId == null) {
            throw new RegraNegocioException(
                    "O cartão de crédito é obrigatório quando o meio de pagamento for CREDITO.");
        }

        lancamento.setCartaoCreditoId(cartaoCreditoId);
        // Todo lançamento de cartão nasce PENDENTE e só é efetivado quando a fatura for paga
        lancamento.setStatus(StatusLancamento.PENDENTE);
        lancamento.setDataEfetivacao(null);
    }

    @Override
    protected List<Lancamento> gerarESalvarLancamentos(Lancamento lancamentoBase, LancamentoRequestDTO dto) {
        CartaoCredito cartao = faturaService.obterCartaoAtivo(lancamentoBase.getCartaoCreditoId());
        List<Lancamento> lancamentosSalvos = new ArrayList<>();

        if (lancamentoBase.getTipoRecorrencia() == TipoRecorrencia.PARCELADO) {
            int parcelas = dto.quantidadeParcelas();
            BigDecimal quantidadeBD = BigDecimal.valueOf(parcelas);
            BigDecimal valorInformado = dto.valor();

            BigDecimal valorParcelaBase;
            BigDecimal restoCentavos = BigDecimal.ZERO;
            BigDecimal valorTotalCompra;

            if (dto.tipoValorParcelamento() == TipoValorParcelamento.VALOR_PARCELA) {
                valorParcelaBase = valorInformado;
                valorTotalCompra = valorInformado.multiply(quantidadeBD);
            } else {
                valorParcelaBase = valorInformado.divide(quantidadeBD, 2, RoundingMode.DOWN);
                restoCentavos = valorInformado.subtract(valorParcelaBase.multiply(quantidadeBD));
                valorTotalCompra = valorInformado;
            }

            faturaService.validarLimiteDisponivel(cartao, valorTotalCompra);

            Long grupoId = null;

            for (int i = 1; i <= parcelas; i++) {
                Lancamento p = copiarLancamento(lancamentoBase);
                p.setParcelaAtual(i);
                p.setTotalParcelas(parcelas);
                p.setGrupoParcelamentoId(grupoId);
                p.setValor(i == 1 ? valorParcelaBase.add(restoCentavos) : valorParcelaBase);
                p.setStatus(StatusLancamento.PENDENTE);
                p.setDataEfetivacao(null);
                // Mantém a mesma dataLancamento (data da compra original) em todas as parcelas
                p.setDataLancamento(lancamentoBase.getDataLancamento());

                Fatura faturaParcela = faturaService.obterOuCriarFaturaAbertaParaCompra(
                        cartao,
                        lancamentoBase.getDataLancamento(),
                        i - 1
                );
                p.setFaturaId(faturaParcela.getId());
                p.setDataVencimento(faturaParcela.getDataVencimento());

                Lancamento salvo = repository.save(p);
                if (i == 1) {
                    grupoId = salvo.getId();
                    salvo.setGrupoParcelamentoId(grupoId);
                }
                lancamentosSalvos.add(salvo);
            }
        } else {
            faturaService.validarLimiteDisponivel(cartao, lancamentoBase.getValor());

            lancamentoBase.setParcelaAtual(1);
            lancamentoBase.setTotalParcelas(1);
            lancamentoBase.setGrupoParcelamentoId(null);

            Fatura fatura = faturaService.obterOuCriarFaturaAbertaParaCompra(
                    cartao,
                    lancamentoBase.getDataLancamento(),
                    0
            );
            lancamentoBase.setFaturaId(fatura.getId());
            lancamentoBase.setDataVencimento(fatura.getDataVencimento());

            lancamentosSalvos.add(repository.save(lancamentoBase));
        }

        return lancamentosSalvos;
    }

    @Override
    protected void posProcessar(List<Lancamento> lancamentosSalvos) {
        for (Lancamento salvo : lancamentosSalvos) {
            faturaService.adicionarValorNaFatura(salvo.getFaturaId(), salvo.getValor());
        }
    }

    @Override
    protected void reverterImpactoFinanceiro(Lancamento lancamentoAntigo) {
        if (lancamentoAntigo.getFaturaId() != null) {
            faturaService.removerValorDaFatura(lancamentoAntigo.getFaturaId(), lancamentoAntigo.getValor());
        }
    }

    @Override
    protected Lancamento atualizarESalvarLancamento(Lancamento lancamento) {
        Lancamento salvo = repository.save(lancamento);
        repository.flush();
        faturaService.adicionarValorNaFatura(salvo.getFaturaId(), salvo.getValor());
        return salvo;
    }

    @Override
    protected void atualizarDemaisParcelasPendentesDoGrupo(Lancamento lancamentoAlvo, LancamentoUpdateDTO dto) {
        List<Lancamento> pendentesDoGrupo = repository.findByGrupoParcelamentoIdAndStatusAndIdNot(
                lancamentoAlvo.getGrupoParcelamentoId(),
                StatusLancamento.PENDENTE,
                lancamentoAlvo.getId()
        );
        if (pendentesDoGrupo.isEmpty()) {
            return;
        }

        List<Lancamento> parcelasEmFaturaAberta = new ArrayList<>();
        for (Lancamento pendente : pendentesDoGrupo) {
            if (pendente.getFaturaId() != null) {
                Fatura fatura = faturaService.buscarEntidadePorId(pendente.getFaturaId());
                if (fatura.isAberta()) {
                    reverterImpactoFinanceiro(pendente);
                    parcelasEmFaturaAberta.add(pendente);
                }
            }
        }

        if (parcelasEmFaturaAberta.isEmpty()) {
            return;
        }

        CartaoCredito cartao = faturaService.obterCartaoAtivo(lancamentoAlvo.getCartaoCreditoId());
        BigDecimal valorTotalAdicional = lancamentoAlvo.getValor()
                .multiply(BigDecimal.valueOf(parcelasEmFaturaAberta.size()));
        faturaService.validarLimiteDisponivel(cartao, valorTotalAdicional);

        for (Lancamento pendente : parcelasEmFaturaAberta) {
            boolean mudouCartao = !lancamentoAlvo.getCartaoCreditoId().equals(pendente.getCartaoCreditoId());

            pendente.setDescricao(lancamentoAlvo.getDescricao());
            pendente.setValor(lancamentoAlvo.getValor());
            pendente.setCategoria(lancamentoAlvo.getCategoria());
            pendente.setSubcategoria(lancamentoAlvo.getSubcategoria());
            pendente.setObservacoes(lancamentoAlvo.getObservacoes());
            pendente.setCartaoCreditoId(lancamentoAlvo.getCartaoCreditoId());
            pendente.setDocumentoFiscalId(lancamentoAlvo.getDocumentoFiscalId());

            if (mudouCartao) {
                int offsetMeses = (pendente.getParcelaAtual() != null && pendente.getParcelaAtual() > 1)
                        ? pendente.getParcelaAtual() - 1
                        : 0;
                Fatura novaFatura = faturaService.obterOuCriarFaturaAbertaParaCompra(
                        cartao,
                        pendente.getDataLancamento(),
                        offsetMeses
                );
                pendente.setFaturaId(novaFatura.getId());
                pendente.setDataVencimento(novaFatura.getDataVencimento());
            }

            faturaService.adicionarValorNaFatura(pendente.getFaturaId(), pendente.getValor());
        }

        repository.saveAll(parcelasEmFaturaAberta);
        repository.flush();
    }

    @Override
    protected void excluirDemaisParcelasPendentesDoGrupo(Lancamento lancamentoAlvo) {
        List<Lancamento> pendentesDoGrupo = repository.findByGrupoParcelamentoIdAndStatusAndIdNot(
                lancamentoAlvo.getGrupoParcelamentoId(),
                StatusLancamento.PENDENTE,
                lancamentoAlvo.getId()
        );
        if (!pendentesDoGrupo.isEmpty()) {
            List<Lancamento> parcelasEmFaturaAberta = new ArrayList<>();
            for (Lancamento pendente : pendentesDoGrupo) {
                if (pendente.getFaturaId() != null) {
                    Fatura fatura = faturaService.buscarEntidadePorId(pendente.getFaturaId());
                    if (fatura.isAberta()) {
                        reverterImpactoFinanceiro(pendente);
                        parcelasEmFaturaAberta.add(pendente);
                    }
                }
            }
            if (!parcelasEmFaturaAberta.isEmpty()) {
                repository.deleteAll(parcelasEmFaturaAberta);
            }
        }
    }

    @Override
    protected void posProcessarAtualizacao(Lancamento salvo, LancamentoUpdateDTO dto) {
    }

    @Override
    protected void posProcessarExclusao(Lancamento lancamento) {
    }
}

