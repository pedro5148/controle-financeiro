package meufinanceiro_api.financeiro.service.strategy;

import jakarta.persistence.EntityNotFoundException;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.domain.Subcategoria;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.LancamentoMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.SubcategoriaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public abstract class AbstractLancamentoStrategy implements LancamentoStrategy{
    protected final LancamentoRepository repository;
    protected final ContaRepository contaRepository;
    protected final CategoriaRepository categoriaRepository;
    protected final SubcategoriaRepository subcategoriaRepository;
    protected final LancamentoMapper mapper;

    public AbstractLancamentoStrategy(LancamentoRepository repository,
                                      ContaRepository contaRepository,
                                      CategoriaRepository categoriaRepository,
                                      SubcategoriaRepository subcategoriaRepository,
                                      LancamentoMapper mapper) {
        this.repository = repository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public List<LancamentoResponseDTO> processar(LancamentoRequestDTO dto) {
        Lancamento lancamentoBase = mapper.toEntity(dto);
        preencherEntidadesComuns(lancamentoBase, dto);
        preencherEntidadesEspecificas(lancamentoBase, dto);
        List<Lancamento> lancamentosSalvos = gerarESalvarLancamentos(lancamentoBase, dto);
        posProcessar(lancamentosSalvos);

        return lancamentosSalvos.stream().map(mapper::toResponseDTO).toList();
    }

    @Transactional
    public LancamentoResponseDTO atualizar(Lancamento lancamentoExistente, LancamentoRequestDTO dto) {
        // Fase 1: Estorno financeiro do estado atual (antes da mutação)
        reverterImpactoFinanceiro(lancamentoExistente);

        // Fase 2: Mesclagem e validação dos novos dados
        mapper.atualizarEntidade(dto, lancamentoExistente);
        preencherEntidadesComuns(lancamentoExistente, dto);
        preencherEntidadesEspecificas(lancamentoExistente, dto);

        // Fase 3: Efetivação do novo estado financeiro e persistência
        Lancamento salvo = atualizarESalvarLancamento(lancamentoExistente);
        posProcessarAtualizacao(salvo, dto);

        return mapper.toResponseDTO(salvo);
    }

    @Transactional
    public void excluir(Lancamento lancamento) {
        // 1: Devolve o dinheiro para a conta (Se estava efetivado)
        reverterImpactoFinanceiro(lancamento);

        // 2: Remove do banco de dados
        repository.delete(lancamento);
        repository.flush();

        posProcessarExclusao(lancamento);
    }

    protected abstract void posProcessarExclusao(Lancamento lancamento);
    protected abstract void reverterImpactoFinanceiro(Lancamento lancamentoAntigo);
    protected abstract Lancamento atualizarESalvarLancamento(Lancamento lancamento);
    protected abstract void posProcessarAtualizacao(Lancamento salvo, LancamentoRequestDTO dto);
    protected void preencherEntidadesComuns(Lancamento lancamento, LancamentoRequestDTO dto) {
        if (dto.contaId() != null) {
            lancamento.setConta(contaRepository.findById(dto.contaId())
                    .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada")));
        } else {
            lancamento.setConta(null);
        }

        if (dto.contaDestinoId() != null) {
            lancamento.setContaDestino(contaRepository.findById(dto.contaDestinoId())
                    .orElseThrow(() -> new EntityNotFoundException("Conta de destino não encontrada")));
        } else {
            lancamento.setContaDestino(null);
        }

        if (dto.categoriaId() != null) {
            lancamento.setCategoria(categoriaRepository.findById(dto.categoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada")));
        } else {
            throw new RegraNegocioException("Categoria é obrigatória.");
        }

        if (dto.subcategoriaId() != null) {
            Subcategoria sub = subcategoriaRepository.findById(dto.subcategoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada"));

            if (!sub.getCategoria().getId().equals(dto.categoriaId())) {
                throw new RegraNegocioException("A Subcategoria informada não pertence à Categoria selecionada.");
            }
            lancamento.setSubcategoria(sub);
        } else {
            lancamento.setSubcategoria(null);
        }
    }
    protected abstract void preencherEntidadesEspecificas(Lancamento lancamento, LancamentoRequestDTO dto);
    protected abstract void posProcessar(List<Lancamento> lancamentosSalvos);
    protected abstract List<Lancamento> gerarESalvarLancamentos(Lancamento lancamentoBase, LancamentoRequestDTO dto);
    protected Lancamento copiarLancamento(Lancamento base) {
        Lancamento copia = new Lancamento();
        copia.setTipo(base.getTipo());
        copia.setMeioPagamento(base.getMeioPagamento());
        copia.setDescricao(base.getDescricao());
        copia.setValor(base.getValor());
        copia.setConta(base.getConta());
        copia.setContaDestino(base.getContaDestino());
        copia.setCategoria(base.getCategoria());
        copia.setSubcategoria(base.getSubcategoria());
        copia.setDataLancamento(base.getDataLancamento());
        copia.setDataVencimento(base.getDataVencimento());
        copia.setDataEfetivacao(base.getDataEfetivacao());
        copia.setStatus(base.getStatus());
        copia.setObservacoes(base.getObservacoes());
        copia.setTipoRecorrencia(base.getTipoRecorrencia());
        copia.setParcelaAtual(base.getParcelaAtual());
        copia.setLancamentoParcelado(base.getLancamentoParcelado());
        copia.setFaturaId(base.getFaturaId());
        copia.setCartaoCreditoId(base.getCartaoCreditoId());
        copia.setDocumentoFiscalId(base.getDocumentoFiscalId());
        return copia;
    }
}