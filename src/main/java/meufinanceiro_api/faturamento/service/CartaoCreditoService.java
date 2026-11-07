package meufinanceiro_api.faturamento.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.faturamento.domain.CartaoCredito;
import meufinanceiro_api.faturamento.dto.CartaoCreditoRequestDTO;
import meufinanceiro_api.faturamento.dto.CartaoCreditoResponseDTO;
import meufinanceiro_api.faturamento.mapper.CartaoCreditoMapper;
import meufinanceiro_api.faturamento.repository.CartaoCreditoRepository;
import meufinanceiro_api.faturamento.repository.FaturaRepository;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartaoCreditoService {

    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final FaturaRepository faturaRepository;
    private final ContaRepository contaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final CartaoCreditoMapper cartaoCreditoMapper;

    @Transactional(readOnly = true)
    public List<CartaoCreditoResponseDTO> listarTodos(Boolean apenasAtivos) {
        List<CartaoCredito> cartoes = Boolean.TRUE.equals(apenasAtivos)
                ? cartaoCreditoRepository.findAllByAtivoTrueOrderByPrincipalDescNomeAsc()
                : cartaoCreditoRepository.findAllByOrderByPrincipalDescNomeAsc();

        if (cartoes.isEmpty()) {
            return List.of();
        }

        List<Long> cartaoIds = cartoes.stream().map(CartaoCredito::getId).toList();
        Map<Long, BigDecimal> limitesPorCartao = faturaRepository.somarLimitesUtilizadosPorCartoes(cartaoIds)
                .stream()
                .collect(Collectors.toMap(
                        FaturaRepository.LimiteUtilizadoCartaoProjection::getCartaoId,
                        p -> p.getLimiteUtilizado() != null ? p.getLimiteUtilizado() : BigDecimal.ZERO
                ));

        return cartoes.stream()
                .map(cartao -> montarResponseComLimite(
                        cartao,
                        limitesPorCartao.getOrDefault(cartao.getId(), BigDecimal.ZERO)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CartaoCreditoResponseDTO buscarPorId(Long id) {
        CartaoCredito cartao = buscarEntidadePorId(id);
        return montarResponseComLimites(cartao);
    }

    @Transactional(readOnly = true)
    public CartaoCredito buscarEntidadePorId(Long id) {
        return cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado."));
    }

    @Transactional(readOnly = true)
    public CartaoCredito obterCartaoAtivo(Long cartaoCreditoId) {
        CartaoCredito cartao = buscarEntidadePorId(cartaoCreditoId);
        cartao.validarAtivoParaLancamento();
        return cartao;
    }

    @Transactional(readOnly = true)
    public void validarLimiteDisponivel(CartaoCredito cartao, BigDecimal valorAdicional) {
        if (valorAdicional == null || valorAdicional.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal limiteUtilizado = obterLimiteUtilizado(cartao.getId());
        cartao.validarLimiteDisponivel(limiteUtilizado, valorAdicional);
    }

    @Transactional
    public CartaoCreditoResponseDTO criar(CartaoCreditoRequestDTO dto) {
        if (cartaoCreditoRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new RegraNegocioException("Já existe um cartão de crédito com o nome informado.");
        }

        validarContaPadrao(dto.contaPadraoId());

        CartaoCredito cartao = cartaoCreditoMapper.toEntity(dto);

        boolean ehPrimeiroCartao = cartaoCreditoRepository.count() == 0;
        boolean ehPrincipal = ehPrimeiroCartao || Boolean.TRUE.equals(dto.principal());
        boolean ativo = dto.ativo() == null || dto.ativo();

        cartao.configurarEstadoInicial(ehPrincipal, ativo);

        if (ehPrincipal && !ehPrimeiroCartao) {
            cartaoCreditoRepository.removerFlagPrincipalDeOutrosCartoes(null);
        }

        CartaoCredito salvo = cartaoCreditoRepository.save(cartao);
        return montarResponseComLimite(salvo, BigDecimal.ZERO);
    }

    @Transactional
    public CartaoCreditoResponseDTO atualizar(Long id, CartaoCreditoRequestDTO dto) {
        if (cartaoCreditoRepository.existsByNomeIgnoreCaseAndIdNot(dto.nome(), id)) {
            throw new RegraNegocioException("Já existe um cartão de crédito com o nome informado.");
        }

        validarContaPadrao(dto.contaPadraoId());

        CartaoCredito existente = buscarEntidadePorId(id);
        boolean promoverAPrincipal = Boolean.TRUE.equals(dto.principal()) && !existente.isPrincipal();

        if (dto.principal() != null) {
            existente.atualizarPrincipal(dto.principal());
        }

        if (dto.ativo() != null) {
            existente.atualizarAtivo(dto.ativo());
        }

        if (promoverAPrincipal) {
            cartaoCreditoRepository.removerFlagPrincipalDeOutrosCartoes(id);
            existente = buscarEntidadePorId(id);
            existente.atualizarPrincipal(true);
            if (dto.ativo() != null) {
                existente.atualizarAtivo(dto.ativo());
            }
        }

        cartaoCreditoMapper.atualizarEntidade(dto, existente);

        CartaoCredito salvo = cartaoCreditoRepository.save(existente);
        return montarResponseComLimites(salvo);
    }

    @Transactional
    public void inativar(Long id) {
        CartaoCredito existente = buscarEntidadePorId(id);
        existente.inativar();
        cartaoCreditoRepository.save(existente);
    }

    @Transactional
    public void ativar(Long id) {
        CartaoCredito existente = buscarEntidadePorId(id);
        existente.ativar();
        cartaoCreditoRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        CartaoCredito existente = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado para exclusão."));

        if (lancamentoRepository.existsByCartaoCreditoId(id) || faturaRepository.existsByCartaoId(id)) {
            throw new RegraNegocioException("Não é possível excluir o cartão de crédito, pois existem faturas ou lançamentos vinculados a ele.");
        }

        existente.validarExclusao();

        cartaoCreditoRepository.delete(existente);
    }

    private void validarContaPadrao(Long contaPadraoId) {
        Conta conta = contaRepository.findById(contaPadraoId)
                .orElseThrow(() -> new EntityNotFoundException("Conta padrão não encontrada."));
        if (!conta.isAtivo()) {
            throw new RegraNegocioException("A conta padrão do cartão de crédito deve estar ativa.");
        }
    }

    private BigDecimal obterLimiteUtilizado(Long cartaoId) {
        BigDecimal limiteUtilizado = faturaRepository.somarLimiteUtilizadoPorCartao(cartaoId);
        return (limiteUtilizado != null) ? limiteUtilizado : BigDecimal.ZERO;
    }

    private CartaoCreditoResponseDTO montarResponseComLimites(CartaoCredito cartao) {
        BigDecimal limiteUtilizado = obterLimiteUtilizado(cartao.getId());
        return montarResponseComLimite(cartao, limiteUtilizado);
    }

    private CartaoCreditoResponseDTO montarResponseComLimite(CartaoCredito cartao, BigDecimal limiteUtilizado) {
        BigDecimal limiteDisponivel = cartao.calcularLimiteDisponivel(limiteUtilizado);
        return cartaoCreditoMapper.toResponseDTO(cartao, limiteUtilizado, limiteDisponivel);
    }
}

