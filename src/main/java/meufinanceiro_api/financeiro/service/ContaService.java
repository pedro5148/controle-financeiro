package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.dto.ContaRequestDTO;
import meufinanceiro_api.financeiro.dto.ContaResponseDTO;
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

    public List<ContaResponseDTO> listarTodasAsContas() {
        return contaRepository.findAllByOrderByPadraoDescNomeAsc().stream()
                .map(contaMapper::toResponseDTO)
                .collect(Collectors.toList());
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

        if (conta.isPadrao()) {
            contaRepository.removerFlagPadraoDeOutrasContas(null);
        }

        return contaMapper.toResponseDTO(contaRepository.save(conta));
    }

    @Transactional
    public ContaResponseDTO atualizar(Long id, ContaRequestDTO dto) {
        Conta existente = contaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));

        String novoNomeFormatado = dto.nome().trim();
        if (!existente.getNome().equalsIgnoreCase(novoNomeFormatado) &&
            contaRepository.existsByNomeIgnoreCase(novoNomeFormatado)){
            throw new RegraNegocioException("Ja existe uma conta com o nome informado");
        }

        BigDecimal novoSaldoInicial = dto.saldoInicial() != null ? dto.saldoInicial() : BigDecimal.ZERO;
        BigDecimal variacao = novoSaldoInicial.subtract(existente.getSaldoInicial());

        existente.setSaldoAtual(existente.getSaldoAtual().add(variacao));
        existente.setSaldoInicial(novoSaldoInicial);

        existente.setNome(novoNomeFormatado);

        if (dto.padrao() && !existente.isPadrao()) {
            contaRepository.removerFlagPadraoDeOutrasContas(id);
        }
        existente.setPadrao(dto.padrao());

        return contaMapper.toResponseDTO(contaRepository.save(existente));
    }

    @Transactional
    public void excluir(Long id) {
        Conta existente = contaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada para exclusão."));

        // Integridade Relacional
        if (lancamentoRepository.existsByContaId(id) || lancamentoRepository.existsByContaDestinoId(id)) {
            throw new RegraNegocioException("Não é possível excluir a conta, pois existem lançamentos financeiros vinculados a ela.");
        }

        // Regra de Negócio
        if (existente.isPadrao()) {
            throw new RegraNegocioException("Não é possível excluir a conta padrão. Defina outra conta como padrão antes de excluí-la.");
        }

        contaRepository.delete(existente);
    }
}