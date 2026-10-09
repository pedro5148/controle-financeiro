package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.LancamentoMapper;
import meufinanceiro_api.financeiro.repository.ContaRepository;
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

    private final LancamentoRepository repository;
    //private final FaturaService faturaService;
    private final ContaRepository contaRepository;
    //private final CategoriaRepository categoriaRepository;
    //private final SubcategoriaRepository subcategoriaRepository;
    private final LancamentoMapper mapper;
    private final List<LancamentoStrategy> strategies;

    public Page<LancamentoResponseDTO> listar(String descricao,
            TipoLancamento tipo,
            Integer mes,
            Integer ano,
            Pageable pageable) {
        Specification<Lancamento> spec = LancamentoSpecification
                .filtroAvancado(descricao, tipo, mes, ano);
        Page<Lancamento> paginasEntidade = repository.findAll(spec, pageable);
        return paginasEntidade.map(mapper::toResponseDTO);
    }

    @Transactional
    public List<LancamentoResponseDTO> criarLancamentos(LancamentoRequestDTO dto) {
        LancamentoStrategy strategy = strategies.stream()
                .filter(s -> s.isApplicable(dto))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Estratégia não suportada para o lançamento solicitado."));
        return strategy.processar(dto);
    }

    @Transactional
    public LancamentoResponseDTO atualizar(Long id, LancamentoRequestDTO dto) {
        Lancamento existente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado"));

        LancamentoStrategy strategy = strategies.stream()
                .filter(s -> s.isApplicable(dto))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Estratégia não suportada para a atualização solicitada."));

        return strategy.atualizar(existente, dto);
    }

    @Transactional
    public void excluir(Long id) {
        Lancamento existente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado para exclusão"));

        LancamentoStrategy strategy = strategies.stream()
                .filter(s -> s.isApplicable(existente))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Estratégia não suportada para exclusão."));

        strategy.excluir(existente);
    }
}
