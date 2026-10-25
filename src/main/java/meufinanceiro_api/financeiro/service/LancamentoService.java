package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.dto.CategoriaResponseDTO;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
import meufinanceiro_api.financeiro.dto.LancamentoSugestaoDTO;
import meufinanceiro_api.financeiro.dto.LancamentoUpdateDTO;
import meufinanceiro_api.financeiro.dto.SubcategoriaResponseDTO;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
            Long cartaoCreditoId,
            Long faturaId,
            Integer mes,
            Integer ano,
            Pageable pageable) {
        Specification<Lancamento> spec = LancamentoSpecification
                .filtroAvancado(descricao, tipo, status, contaId, categoriaId, subcategoriaId, grupoParcelamentoId, cartaoCreditoId, faturaId, mes, ano);
        Page<Lancamento> paginasEntidade = lancamentoRepository.findAll(spec, pageable);
        return paginasEntidade.map(lancamentoMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<LancamentoSugestaoDTO> sugerirDescricoes(String termo, TipoLancamento tipo, int limite) {
        if (termo == null) {
            return List.of();
        }
        String termoLimpo = termo.trim().replaceAll("\\s+", " ");
        if (termoLimpo.length() < 3) {
            return List.of();
        }
        int limiteSeguro = Math.clamp(limite, 1, 20);
        String tipoStr = (tipo != null) ? tipo.name() : null;

        return lancamentoRepository.buscarSugestoesPorDescricao(termoLimpo, tipoStr, limiteSeguro)
                .stream()
                .map(p -> {
                    CategoriaResponseDTO cat = (p.getCategoriaId() != null && Boolean.TRUE.equals(p.getCategoriaAtivo()))
                            ? new CategoriaResponseDTO(p.getCategoriaId(), p.getCategoriaNome(), p.getCategoriaAtivo(), null)
                            : null;
                    SubcategoriaResponseDTO sub = (cat != null && p.getSubcategoriaId() != null && Boolean.TRUE.equals(p.getSubcategoriaAtivo()))
                            ? new SubcategoriaResponseDTO(p.getSubcategoriaId(), p.getSubcategoriaNome(), p.getSubcategoriaAtivo())
                            : null;
                    return new LancamentoSugestaoDTO(p.getDescricao(), cat, sub);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public LancamentoResponseDTO buscarPorId(Long id) {
        return lancamentoRepository.findById(id)
                .map(lancamentoMapper::toResponseDTO)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado."));
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
    public LancamentoResponseDTO atualizar(Long id, LancamentoUpdateDTO dto, boolean atualizarPendentesDoGrupo) {
        Lancamento existente = lancamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado."));

        LancamentoStrategy strategy = lancamentoStrategies.stream()
                .filter(s -> s.isApplicable(existente))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Estratégia não suportada para o lançamento original."));

        if (!strategy.isApplicable(dto)) {
            throw new RegraNegocioException(
                    "Não é permitido converter um lançamento de conta bancária para cartão de crédito (ou vice-versa). Exclua o lançamento e crie um novo.");
        }

        return strategy.atualizar(existente, dto, atualizarPendentesDoGrupo);
    }

    @Transactional
    public LancamentoResponseDTO efetivar(Long id, LocalDate dataEfetivacao) {
        Lancamento lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado."));

        if (lancamento.getCartaoCreditoId() != null) {
            throw new RegraNegocioException(
                    "Lançamentos no cartão de crédito só podem ser efetivados por meio do pagamento da fatura.");
        }

        if (lancamento.getStatus() == StatusLancamento.EFETIVADO) {
            throw new RegraNegocioException("Este lançamento já se encontra efetivado.");
        }

        if (lancamento.getConta() == null || !lancamento.getConta().isAtivo()) {
            throw new RegraNegocioException("Não é possível efetivar o lançamento em uma conta inativa ou inexistente.");
        }

        BigDecimal valor = lancamento.getValor();
        switch (lancamento.getTipo()) {
            case RECEITA -> lancamento.getConta().creditar(valor);
            case DESPESA -> lancamento.getConta().debitar(valor);
            case TRANSFERENCIA -> {
                if (lancamento.getContaDestino() == null || !lancamento.getContaDestino().isAtivo()) {
                    throw new RegraNegocioException("Não é possível efetivar a transferência para uma conta de destino inativa ou inexistente.");
                }
                lancamento.getConta().debitar(valor);
                lancamento.getContaDestino().creditar(valor);
            }
        }

        lancamento.setStatus(StatusLancamento.EFETIVADO);
        lancamento.setDataEfetivacao(dataEfetivacao != null ? dataEfetivacao : LocalDate.now());

        Lancamento salvo = lancamentoRepository.saveAndFlush(lancamento);
        return lancamentoMapper.toResponseDTO(salvo);
    }

    @Transactional
    public LancamentoResponseDTO marcarComoPendente(Long id) {
        Lancamento lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado."));

        if (lancamento.getCartaoCreditoId() != null) {
            throw new RegraNegocioException(
                    "O status de lançamentos no cartão de crédito é controlado pelo pagamento da fatura.");
        }

        if (lancamento.getStatus() == StatusLancamento.PENDENTE) {
            throw new RegraNegocioException("Este lançamento já se encontra pendente.");
        }

        BigDecimal valor = lancamento.getValor();
        switch (lancamento.getTipo()) {
            case DESPESA -> lancamento.getConta().creditar(valor);
            case RECEITA -> lancamento.getConta().debitar(valor);
            case TRANSFERENCIA -> {
                lancamento.getConta().creditar(valor);
                if (lancamento.getContaDestino() != null) {
                    lancamento.getContaDestino().debitar(valor);
                }
            }
        }

        lancamento.setStatus(StatusLancamento.PENDENTE);
        lancamento.setDataEfetivacao(null);

        Lancamento salvo = lancamentoRepository.saveAndFlush(lancamento);
        return lancamentoMapper.toResponseDTO(salvo);
    }

    @Transactional
    public void excluir(Long id, boolean excluirPendentesDoGrupo, boolean excluirProximos) {
        if (excluirPendentesDoGrupo && excluirProximos) {
            throw new RegraNegocioException("Informe apenas um alcance de exclusão.");
        }
        if (!excluirProximos) {
            excluir(id, excluirPendentesDoGrupo);
            return;
        }
        Lancamento selecionado = lancamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado para exclusão."));
        if (selecionado.getGrupoParcelamentoId() == null || selecionado.getParcelaAtual() == null) {
            throw new RegraNegocioException("A exclusão dos próximos lançamentos exige uma parcela de um parcelamento.");
        }

        LancamentoStrategy strategy = lancamentoStrategies.stream()
                .filter(s -> s.isApplicable(selecionado))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Estratégia não suportada para exclusão."));

        List<Lancamento> parcelas = lancamentoRepository
                .findByGrupoParcelamentoIdAndParcelaAtualGreaterThanEqualOrderByParcelaAtualAsc(
                        selecionado.getGrupoParcelamentoId(), selecionado.getParcelaAtual());
        // A mesma transação estorna cada parcela e desfaz tudo se alguma exclusão falhar.
        for (Lancamento parcela : parcelas) {
            strategy.excluir(parcela, false);
        }
    }

    @Transactional
    public void excluir(Long id, boolean excluirPendentesDoGrupo) {
        Lancamento existente = lancamentoRepository.findById(id)
                .orElseGet(() -> {
                    if (excluirPendentesDoGrupo) {
                        return lancamentoRepository.findFirstByGrupoParcelamentoIdAndStatus(id, StatusLancamento.PENDENTE)
                                .orElseThrow(() -> new EntityNotFoundException("Lançamento ou grupo de parcelas pendentes não encontrado para exclusão."));
                    }
                    throw new EntityNotFoundException("Lançamento não encontrado para exclusão.");
                });

        LancamentoStrategy strategy = lancamentoStrategies.stream()
                .filter(s -> s.isApplicable(existente))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Estratégia não suportada para exclusão."));

        strategy.excluir(existente, excluirPendentesDoGrupo);
    }
}
