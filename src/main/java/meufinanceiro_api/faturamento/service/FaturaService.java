package meufinanceiro_api.faturamento.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.faturamento.domain.CartaoCredito;
import meufinanceiro_api.faturamento.domain.Fatura;
import meufinanceiro_api.faturamento.domain.PagamentoFatura;
import meufinanceiro_api.faturamento.dto.FaturaResponseDTO;
import meufinanceiro_api.faturamento.dto.PagamentoFaturaRequestDTO;
import meufinanceiro_api.faturamento.dto.PagamentoFaturaResponseDTO;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import meufinanceiro_api.faturamento.mapper.FaturaMapper;
import meufinanceiro_api.faturamento.mapper.PagamentoFaturaMapper;
import meufinanceiro_api.faturamento.repository.CartaoCreditoRepository;
import meufinanceiro_api.faturamento.repository.FaturaRepository;
import meufinanceiro_api.faturamento.repository.PagamentoFaturaRepository;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FaturaService {

    private final FaturaRepository faturaRepository;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final PagamentoFaturaRepository pagamentoFaturaRepository;
    private final ContaRepository contaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final FaturaMapper faturaMapper;
    private final PagamentoFaturaMapper pagamentoFaturaMapper;

    @Transactional(readOnly = true)
    public List<FaturaResponseDTO> listar(Long cartaoCreditoId, String mesAno, StatusFatura status) {
        String mesAnoFiltro = (mesAno != null && !mesAno.isBlank()) ? mesAno.trim() : null;
        return faturaRepository.listarComFiltros(cartaoCreditoId, mesAnoFiltro, status)
                .stream()
                .map(faturaMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public FaturaResponseDTO buscarPorId(Long id) {
        Fatura fatura = buscarEntidadePorId(id);
        return faturaMapper.toResponseDTO(fatura);
    }

    @Transactional(readOnly = true)
    public Fatura buscarEntidadePorId(Long id) {
        return faturaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fatura não encontrada."));
    }

    @Transactional(readOnly = true)
    public CartaoCredito obterCartaoAtivo(Long cartaoCreditoId) {
        CartaoCredito cartao = cartaoCreditoRepository.findById(cartaoCreditoId)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado."));
        if (!cartao.isAtivo()) {
            throw new RegraNegocioException("Não é possível realizar lançamentos em um cartão de crédito inativo.");
        }
        return cartao;
    }

    @Transactional(readOnly = true)
    public void validarLimiteDisponivel(CartaoCredito cartao, BigDecimal valorAdicional) {
        if (valorAdicional == null || valorAdicional.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal limiteUtilizado = faturaRepository.somarLimiteUtilizadoPorCartao(cartao.getId());
        if (limiteUtilizado == null) {
            limiteUtilizado = BigDecimal.ZERO;
        }
        BigDecimal limiteDisponivel = cartao.getLimiteTotal().subtract(limiteUtilizado);
        if (valorAdicional.compareTo(limiteDisponivel) > 0) {
            throw new RegraNegocioException(
                    String.format("Limite insuficiente no cartão '%s'. Limite disponível: R$ %s, valor da compra: R$ %s.",
                            cartao.getNome(), limiteDisponivel, valorAdicional)
            );
        }
    }

    /**
     * Resolve a fatura ABERTA correspondente à data da compra e ao deslocamento de meses (para parcelas).
     * Se a fatura calculada para a 1ª parcela já estiver FECHADA ou PAGA, aloca automaticamente na próxima fatura ABERTA.
     */
    @Transactional
    public Fatura obterOuCriarFaturaAbertaParaCompra(CartaoCredito cartao, LocalDate dataCompra, int offsetMeses) {
        YearMonth competenciaBase = cartao.calcularCompetenciaFatura(dataCompra);

        // Se a fatura base da compra já estiver fechada ou paga, avança para a próxima competência aberta
        while (true) {
            String mesAnoTeste = CartaoCredito.formatarMesAno(competenciaBase);
            Optional<Fatura> faturaExistente = faturaRepository.findByCartaoIdAndMesAno(cartao.getId(), mesAnoTeste);
            if (faturaExistente.isPresent() && !faturaExistente.get().isAberta()) {
                competenciaBase = competenciaBase.plusMonths(1);
            } else {
                break;
            }
        }

        YearMonth competenciaAlvo = competenciaBase.plusMonths(offsetMeses);

        // Garante que a competência alvo da parcela também esteja aberta
        while (true) {
            String mesAnoAlvo = CartaoCredito.formatarMesAno(competenciaAlvo);
            Optional<Fatura> faturaAlvoOpt = faturaRepository.findByCartaoIdAndMesAno(cartao.getId(), mesAnoAlvo);
            if (faturaAlvoOpt.isPresent()) {
                Fatura faturaAlvo = faturaAlvoOpt.get();
                if (!faturaAlvo.isAberta()) {
                    competenciaAlvo = competenciaAlvo.plusMonths(1);
                    continue;
                }
                return faturaAlvo;
            }

            Fatura novaFatura = new Fatura();
            novaFatura.setCartao(cartao);
            novaFatura.setMesAno(mesAnoAlvo);
            novaFatura.setValorTotal(BigDecimal.ZERO);
            novaFatura.setValorPago(BigDecimal.ZERO);
            novaFatura.setStatus(StatusFatura.ABERTA);
            novaFatura.setDataFechamento(cartao.calcularDataFechamento(competenciaAlvo));
            novaFatura.setDataVencimento(cartao.calcularDataVencimento(competenciaAlvo));
            return faturaRepository.save(novaFatura);
        }
    }

    @Transactional
    public Fatura adicionarValorNaFatura(Long faturaId, BigDecimal valor) {
        Fatura fatura = buscarEntidadePorId(faturaId);
        fatura.adicionarValor(valor);
        return faturaRepository.saveAndFlush(fatura);
    }

    @Transactional
    public Fatura removerValorDaFatura(Long faturaId, BigDecimal valor) {
        Fatura fatura = buscarEntidadePorId(faturaId);
        fatura.removerValor(valor);
        return faturaRepository.saveAndFlush(fatura);
    }

    @Transactional
    public FaturaResponseDTO fecharFatura(Long id) {
        Fatura fatura = buscarEntidadePorId(id);
        fatura.fechar();

        if (fatura.getStatus() == StatusFatura.PAGA) {
            LocalDate dataEfetivacao = pagamentoFaturaRepository
                    .findFirstByFaturaIdOrderByDataPagamentoDescIdDesc(fatura.getId())
                    .map(PagamentoFatura::getDataPagamento)
                    .orElse(LocalDate.now());
            atualizarStatusLancamentosDaFatura(fatura.getId(), StatusLancamento.EFETIVADO, dataEfetivacao);
        }

        return faturaMapper.toResponseDTO(faturaRepository.saveAndFlush(fatura));
    }

    @Transactional
    public FaturaResponseDTO reabrirFatura(Long id) {
        Fatura fatura = buscarEntidadePorId(id);
        fatura.reabrir();
        return faturaMapper.toResponseDTO(faturaRepository.saveAndFlush(fatura));
    }

    @Transactional
    public FaturaResponseDTO pagarFatura(Long id, PagamentoFaturaRequestDTO dto) {
        Fatura fatura = buscarEntidadePorId(id);

        BigDecimal valorPagamento = (dto != null && dto.valor() != null)
                ? dto.valor()
                : fatura.getSaldoRestante();

        Long contaId = (dto != null && dto.contaId() != null)
                ? dto.contaId()
                : fatura.getCartao().getContaPadraoId();

        LocalDate dataPagamento = (dto != null && dto.dataPagamento() != null)
                ? dto.dataPagamento()
                : LocalDate.now();

        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária para pagamento não encontrada."));

        if (!conta.isAtivo()) {
            throw new RegraNegocioException("Não é possível realizar o pagamento da fatura utilizando uma conta inativa.");
        }

        fatura.registrarPagamento(valorPagamento);
        conta.debitar(valorPagamento);

        PagamentoFatura pagamento = new PagamentoFatura();
        pagamento.setFatura(fatura);
        pagamento.setConta(conta);
        pagamento.setValor(valorPagamento);
        pagamento.setDataPagamento(dataPagamento);
        pagamento.setObservacoes(dto != null ? dto.observacoes() : null);
        pagamentoFaturaRepository.save(pagamento);

        if (fatura.getStatus() == StatusFatura.PAGA) {
            atualizarStatusLancamentosDaFatura(fatura.getId(), StatusLancamento.EFETIVADO, dataPagamento);
        }

        contaRepository.save(conta);
        return faturaMapper.toResponseDTO(faturaRepository.saveAndFlush(fatura));
    }

    @Transactional(readOnly = true)
    public List<PagamentoFaturaResponseDTO> listarPagamentos(Long faturaId) {
        buscarEntidadePorId(faturaId);
        return pagamentoFaturaRepository.findAllByFaturaIdOrderByDataPagamentoDescIdDesc(faturaId)
                .stream()
                .map(pagamentoFaturaMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public FaturaResponseDTO estornarPagamento(Long faturaId, Long pagamentoId) {
        Fatura fatura = buscarEntidadePorId(faturaId);
        PagamentoFatura pagamento = pagamentoFaturaRepository.findByIdAndFaturaId(pagamentoId, faturaId)
                .orElseThrow(() -> new EntityNotFoundException("Registro de pagamento não encontrado para esta fatura."));

        boolean estavaPaga = fatura.getStatus() == StatusFatura.PAGA;

        Conta conta = pagamento.getConta();
        conta.creditar(pagamento.getValor());
        fatura.estornarPagamento(pagamento.getValor());

        if (estavaPaga && fatura.getStatus() != StatusFatura.PAGA) {
            atualizarStatusLancamentosDaFatura(fatura.getId(), StatusLancamento.PENDENTE, null);
        }

        pagamentoFaturaRepository.delete(pagamento);
        contaRepository.save(conta);
        return faturaMapper.toResponseDTO(faturaRepository.saveAndFlush(fatura));
    }

    private void atualizarStatusLancamentosDaFatura(Long faturaId, StatusLancamento novoStatus, LocalDate dataEfetivacao) {
        List<Lancamento> lancamentos = lancamentoRepository.findAllByFaturaId(faturaId);
        if (!lancamentos.isEmpty()) {
            for (Lancamento lancamento : lancamentos) {
                lancamento.setStatus(novoStatus);
                lancamento.setDataEfetivacao(dataEfetivacao);
            }
            lancamentoRepository.saveAll(lancamentos);
        }
    }
}