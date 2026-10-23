package meufinanceiro_api.faturamento.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.faturamento.domain.CartaoCredito;
import meufinanceiro_api.faturamento.domain.Fatura;
import meufinanceiro_api.faturamento.dto.FaturaResponseDTO;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import meufinanceiro_api.faturamento.mapper.FaturaMapper;
import meufinanceiro_api.faturamento.repository.CartaoCreditoRepository;
import meufinanceiro_api.faturamento.repository.FaturaRepository;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
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
    private final FaturaMapper faturaMapper;

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
}