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

    private final ContaRepository repository;
    private final ContaMapper mapper;
    private final LancamentoRepository lancamentoRepository;

    public List<ContaResponseDTO> listarTodasAsContas() {
        return repository.findAll().stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContaResponseDTO criar(ContaRequestDTO dto) {
        Conta conta = mapper.toEntity(dto);

        BigDecimal saldoInicial = dto.saldoInicial() != null ? dto.saldoInicial() : BigDecimal.ZERO;
        conta.setSaldoInicial(saldoInicial);
        conta.setSaldoAtual(saldoInicial);

        if (conta.isPadrao()) {
            repository.removerFlagPadraoDeOutrasContas(null);
        }

        return mapper.toResponseDTO(repository.save(conta));
    }

    @Transactional
    public ContaResponseDTO atualizar(Long id, ContaRequestDTO dto) {
        Conta existente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));

        // Regra Crítica: Recálculo do Saldo Atual baseado na variação do Saldo Inicial
        BigDecimal novoSaldoInicial = dto.saldoInicial() != null ? dto.saldoInicial() : BigDecimal.ZERO;
        BigDecimal variacao = novoSaldoInicial.subtract(existente.getSaldoInicial());

        // Aplica a diferença matemática no saldo atual (seja positiva ou negativa)
        existente.setSaldoAtual(existente.getSaldoAtual().add(variacao));
        existente.setSaldoInicial(novoSaldoInicial);

        existente.setNome(dto.nome());

        // Só aciona o banco se a conta atual estiver virando padrão agora
        if (dto.padrao() && !existente.isPadrao()) {
            repository.removerFlagPadraoDeOutrasContas(id);
        }
        existente.setPadrao(dto.padrao());

        return mapper.toResponseDTO(repository.save(existente));
    }

    @Transactional
    public void excluir(Long id) {
        Conta existente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada para exclusão."));

        // Integridade Relacional
        if (lancamentoRepository.existsByContaId(id) || lancamentoRepository.existsByContaDestinoId(id)) {
            throw new RegraNegocioException("Não é possível excluir a conta, pois existem lançamentos financeiros vinculados a ela.");
        }

        // Regra de Negócio
        if (existente.isPadrao()) {
            throw new RegraNegocioException("Não é possível excluir a conta padrão. Defina outra conta como padrão antes de excluí-la.");
        }

        repository.delete(existente);
    }
}