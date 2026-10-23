package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.faturamento.repository.CartaoCreditoRepository;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.dto.ContaRequestDTO;
import meufinanceiro_api.financeiro.dto.ContaResponseDTO;
import meufinanceiro_api.financeiro.dto.ContaResumoDTO;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.ContaMapper;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final ContaMapper contaMapper;
    private final LancamentoRepository lancamentoRepository;
    private final CartaoCreditoRepository cartaoCreditoRepository;

    @Transactional(readOnly = true)
    public List<ContaResponseDTO> listarTodasAsContas(Boolean apenasAtivas) {
        List<Conta> contas = Boolean.TRUE.equals(apenasAtivas)
                ? contaRepository.findAllByAtivoTrueOrderByPadraoDescNomeAsc()
                : contaRepository.findAllByOrderByPadraoDescNomeAsc();

        return contas.stream()
                .map(contaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContaResponseDTO buscarPorId(Long id) {
        return contaRepository.findById(id)
                .map(contaMapper::toResponseDTO)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));
    }

    @Transactional(readOnly = true)
    public ContaResumoDTO obterResumoContas() {
        BigDecimal saldoTotal = contaRepository.somarSaldoTotalContasAtivas();
        long totalContas = contaRepository.count();
        long totalContasAtivas = contaRepository.countByAtivoTrue();
        return new ContaResumoDTO(saldoTotal, totalContas, totalContasAtivas);
    }

    @Transactional
    public ContaResponseDTO criar(ContaRequestDTO dto) {
        String nomeFormatado = dto.nome().trim();

        if (contaRepository.existsByNomeIgnoreCase(nomeFormatado)){
            throw new RegraNegocioException("Ja existe uma conta com o nome informado.");
        }

        Conta conta = contaMapper.toEntity(dto);
        conta.setNome(nomeFormatado);

        BigDecimal saldoInicial = dto.saldoInicial() != null ? dto.saldoInicial() : BigDecimal.ZERO;
        conta.setSaldoInicial(saldoInicial);
        conta.setSaldoAtual(saldoInicial);

        boolean ehPrimeiraConta = contaRepository.count() == 0;
        boolean ehPadrao = ehPrimeiraConta || Boolean.TRUE.equals(dto.padrao());
        conta.setPadrao(ehPadrao);

        boolean ativo = dto.ativo() == null || dto.ativo();
        conta.setAtivo(ativo);

        if (ehPadrao && !ehPrimeiraConta) {
            contaRepository.removerFlagPadraoDeOutrasContas(null);
        }

        return contaMapper.toResponseDTO(contaRepository.save(conta));
    }

    @Transactional
    public ContaResponseDTO atualizar(Long id, ContaRequestDTO dto) {
        Conta existente = contaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));

        String novoNomeFormatado = dto.nome().trim();
        if (contaRepository.existsByNomeIgnoreCaseAndIdNot(novoNomeFormatado, id)) {
            throw new RegraNegocioException("Já existe uma conta com o nome informado.");
        }

        if (dto.saldoInicial() != null) {
            BigDecimal saldoInicialAntigo = existente.getSaldoInicial() != null ? existente.getSaldoInicial() : BigDecimal.ZERO;
            BigDecimal saldoAtualAntigo = existente.getSaldoAtual() != null ? existente.getSaldoAtual() : BigDecimal.ZERO;
            BigDecimal variacao = dto.saldoInicial().subtract(saldoInicialAntigo);

            existente.setSaldoAtual(saldoAtualAntigo.add(variacao));
            existente.setSaldoInicial(dto.saldoInicial());
        }

        existente.setNome(novoNomeFormatado);

        if (dto.padrao() != null) {
            if (existente.isPadrao() && !dto.padrao()) {
                throw new RegraNegocioException("Não é possível desmarcar a conta padrão. Defina outra conta como padrão para transferir essa configuração.");
            }

            if (dto.padrao() && !existente.isPadrao()) {
                contaRepository.removerFlagPadraoDeOutrasContas(id);
            }
            existente.setPadrao(dto.padrao());
        }

        if (dto.ativo() != null) {
            if (existente.isPadrao() && !dto.ativo()) {
                throw new RegraNegocioException("Não é possível inativar a conta padrão. Defina outra conta como padrão antes de inativá-la.");
            }
            existente.setAtivo(dto.ativo());
        }

        return contaMapper.toResponseDTO(contaRepository.save(existente));
    }

    @Transactional
    public void inativar(Long id) {
        Conta existente = contaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));

        if (existente.isPadrao()) {
            throw new RegraNegocioException("Não é possível inativar a conta padrão. Defina outra conta como padrão antes de inativá-la.");
        }

        existente.setAtivo(false);
        contaRepository.save(existente);
    }

    @Transactional
    public void ativar(Long id) {
        Conta existente = contaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));

        existente.setAtivo(true);
        contaRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        Conta existente = contaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada para exclusão."));

        // Integridade Relacional
        if (lancamentoRepository.existsByContaId(id) || lancamentoRepository.existsByContaDestinoId(id)) {
            throw new RegraNegocioException("Não é possível excluir a conta, pois existem lançamentos financeiros vinculados a ela.");
        }

        if (cartaoCreditoRepository.existsByContaPadraoId(id)) {
            throw new RegraNegocioException("Não é possível excluir a conta, pois ela está configurada como conta padrão de um cartão de crédito.");
        }

        // Regra de Negócio
        if (existente.isPadrao()) {
            throw new RegraNegocioException("Não é possível excluir a conta padrão. Defina outra conta como padrão antes de excluí-la.");
        }

        contaRepository.delete(existente);
    }
}