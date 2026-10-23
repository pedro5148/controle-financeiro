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

        return cartoes.stream()
                .map(this::montarResponseComLimites)
                .toList();
    }

    @Transactional(readOnly = true)
    public CartaoCreditoResponseDTO buscarPorId(Long id) {
        CartaoCredito cartao = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado."));
        return montarResponseComLimites(cartao);
    }

    @Transactional
    public CartaoCreditoResponseDTO criar(CartaoCreditoRequestDTO dto) {
        String nomeFormatado = dto.nome().trim();

        if (cartaoCreditoRepository.existsByNomeIgnoreCase(nomeFormatado)) {
            throw new RegraNegocioException("Já existe um cartão de crédito com o nome informado.");
        }

        validarContaPadrao(dto.contaPadraoId());

        CartaoCredito cartao = cartaoCreditoMapper.toEntity(dto);
        cartao.setNome(nomeFormatado);

        boolean ehPrimeiroCartao = cartaoCreditoRepository.count() == 0;
        boolean ehPrincipal = ehPrimeiroCartao || Boolean.TRUE.equals(dto.principal());
        cartao.setPrincipal(ehPrincipal);

        boolean ativo = dto.ativo() == null || dto.ativo();
        cartao.setAtivo(ativo);

        if (ehPrincipal && !ehPrimeiroCartao) {
            cartaoCreditoRepository.removerFlagPrincipalDeOutrosCartoes(null);
        }

        CartaoCredito salvo = cartaoCreditoRepository.save(cartao);
        return montarResponseComLimites(salvo);
    }

    @Transactional
    public CartaoCreditoResponseDTO atualizar(Long id, CartaoCreditoRequestDTO dto) {
        CartaoCredito existente = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado."));

        String novoNomeFormatado = dto.nome().trim();
        if (cartaoCreditoRepository.existsByNomeIgnoreCaseAndIdNot(novoNomeFormatado, id)) {
            throw new RegraNegocioException("Já existe um cartão de crédito com o nome informado.");
        }

        validarContaPadrao(dto.contaPadraoId());

        existente.setNome(novoNomeFormatado);
        existente.setLimiteTotal(dto.limiteTotal());
        existente.setDiaFechamento(dto.diaFechamento());
        existente.setDiaVencimento(dto.diaVencimento());
        existente.setContaPadraoId(dto.contaPadraoId());
        existente.setBandeira(dto.bandeira());

        if (dto.principal() != null) {
            if (existente.isPrincipal() && !dto.principal()) {
                throw new RegraNegocioException("Não é possível desmarcar o cartão principal. Defina outro cartão como principal para transferir essa configuração.");
            }

            if (dto.principal() && !existente.isPrincipal()) {
                cartaoCreditoRepository.removerFlagPrincipalDeOutrosCartoes(id);
            }
            existente.setPrincipal(dto.principal());
        }

        if (dto.ativo() != null) {
            if (existente.isPrincipal() && !dto.ativo()) {
                throw new RegraNegocioException("Não é possível inativar o cartão principal. Defina outro cartão como principal antes de inativá-lo.");
            }
            existente.setAtivo(dto.ativo());
        }

        CartaoCredito salvo = cartaoCreditoRepository.save(existente);
        return montarResponseComLimites(salvo);
    }

    @Transactional
    public void inativar(Long id) {
        CartaoCredito existente = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado."));

        if (existente.isPrincipal()) {
            throw new RegraNegocioException("Não é possível inativar o cartão principal. Defina outro cartão como principal antes de inativá-lo.");
        }

        existente.setAtivo(false);
        cartaoCreditoRepository.save(existente);
    }

    @Transactional
    public void ativar(Long id) {
        CartaoCredito existente = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado."));

        existente.setAtivo(true);
        cartaoCreditoRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        CartaoCredito existente = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado para exclusão."));

        if (lancamentoRepository.existsByCartaoCreditoId(id) || faturaRepository.existsByCartaoId(id)) {
            throw new RegraNegocioException("Não é possível excluir o cartão de crédito, pois existem faturas ou lançamentos vinculados a ele.");
        }

        if (existente.isPrincipal()) {
            throw new RegraNegocioException("Não é possível excluir o cartão principal. Defina outro cartão como principal antes de excluí-lo.");
        }

        cartaoCreditoRepository.delete(existente);
    }

    private void validarContaPadrao(Long contaPadraoId) {
        Conta conta = contaRepository.findById(contaPadraoId)
                .orElseThrow(() -> new EntityNotFoundException("Conta padrão não encontrada."));
        if (!conta.isAtivo()) {
            throw new RegraNegocioException("A conta padrão do cartão de crédito deve estar ativa.");
        }
    }

    private CartaoCreditoResponseDTO montarResponseComLimites(CartaoCredito cartao) {
        BigDecimal limiteUtilizado = faturaRepository.somarLimiteUtilizadoPorCartao(cartao.getId());
        if (limiteUtilizado == null) {
            limiteUtilizado = BigDecimal.ZERO;
        }
        BigDecimal limiteDisponivel = cartao.getLimiteTotal().subtract(limiteUtilizado);
        return cartaoCreditoMapper.toResponseDTO(cartao, limiteUtilizado, limiteDisponivel);
    }
}

