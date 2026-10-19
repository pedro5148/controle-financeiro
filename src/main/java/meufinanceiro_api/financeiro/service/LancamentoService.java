package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.LancamentoMapper;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.LancamentoSpecification;
import meufinanceiro_api.financeiro.service.strategy.LancamentoStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LancamentoService {

    private final LancamentoRepository lancamentoRepository;
    private final LancamentoMapper lancamentoMapper;
    private final List<LancamentoStrategy> lancamentoStrategies;

    @Transactional(readOnly = true)
    public Page<LancamentoResponseDTO> listar(
            String descricao,
            TipoLancamento tipo,
            StatusLancamento status,
            Long contaId,
            Long categoriaId,
            Long subcategoriaId,
            Long grupoParcelamentoId,
            Integer mes,
            Integer ano,
            Pageable pageable) {
        Specification<Lancamento> spec = LancamentoSpecification
                .filtroAvancado(descricao, tipo, status, contaId, categoriaId, subcategoriaId, grupoParcelamentoId, mes, ano);
        Page<Lancamento> paginasEntidade = lancamentoRepository.findAll(spec, pageable);
        return paginasEntidade.map(lancamentoMapper::toResponseDTO);
    }

    @Transactional
    public List<LancamentoResponseDTO> criarLancamentos(LancamentoRequestDTO dto) {
        LancamentoStrategy strategy = lancamentoStrategies.stream()
                .filter(s -> s.isApplicable(dto))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Estratégia não suportada para o lançamento solicitado."));
        return strategy.processar(dto);
    }

    @Transactional
    public LancamentoResponseDTO atualizar(Long id, LancamentoRequestDTO dto) {
        Lancamento existente = lancamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado"));

        LancamentoStrategy strategy = lancamentoStrategies.stream()
                .filter(s -> s.isApplicable(dto))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Estratégia não suportada para a atualização solicitada."));

        return strategy.atualizar(existente, dto);
    }

    @Transactional
    public void excluir(Long id) {
        Lancamento existente = lancamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado para exclusão"));

        LancamentoStrategy strategy = lancamentoStrategies.stream()
                .filter(s -> s.isApplicable(existente))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Estratégia não suportada para exclusão."));

        strategy.excluir(existente);
    }
}
