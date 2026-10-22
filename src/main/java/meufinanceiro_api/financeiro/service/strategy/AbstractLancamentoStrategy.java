package meufinanceiro_api.financeiro.service.strategy;

import jakarta.persistence.EntityNotFoundException;
import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.domain.Subcategoria;
import meufinanceiro_api.financeiro.dto.LancamentoRequestDTO;
import meufinanceiro_api.financeiro.dto.LancamentoResponseDTO;
import meufinanceiro_api.financeiro.dto.LancamentoUpdateDTO;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.enums.TipoRecorrencia;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.LancamentoMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.SubcategoriaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    @Override
    @Transactional
    public LancamentoResponseDTO atualizar(Lancamento lancamentoExistente, LancamentoUpdateDTO dto) {
        // Fase 1: Estorno financeiro do estado atual (antes da mutação)
        reverterImpactoFinanceiro(lancamentoExistente);

        // Fase 2: Mesclagem e validação dos novos dados (preservando metadados de parcelamento)
        mapper.atualizarEntidade(dto, lancamentoExistente);
        preencherEntidadesComunsAtualizacao(lancamentoExistente, dto);
        preencherEntidadesEspecificasAtualizacao(lancamentoExistente, dto);

        // Fase 3: Efetivação do novo estado financeiro e persistência
        Lancamento salvo = atualizarESalvarLancamento(lancamentoExistente);
        posProcessarAtualizacao(salvo, dto);

        return mapper.toResponseDTO(salvo);
    }

    @Override
    @Transactional
    public void excluir(Lancamento lancamento, boolean excluirPendentesDoGrupo) {
        // 1: Devolve o dinheiro para a conta (Se estava efetivado)
        reverterImpactoFinanceiro(lancamento);

        // 2: Se solicitado e fizer parte de um parcelamento, remove todas as demais parcelas PENDENTES do mesmo grupo
        if (excluirPendentesDoGrupo && lancamento.getGrupoParcelamentoId() != null) {
            List<Lancamento> pendentesDoGrupo = repository.findByGrupoParcelamentoIdAndStatusAndIdNot(
                    lancamento.getGrupoParcelamentoId(),
                    StatusLancamento.PENDENTE,
                    lancamento.getId()
            );
            if (!pendentesDoGrupo.isEmpty()) {
                repository.deleteAll(pendentesDoGrupo);
            }
        }

        // 3: Remove o lançamento alvo do banco de dados
        repository.delete(lancamento);
        repository.flush();

        posProcessarExclusao(lancamento);
    }

    protected abstract void posProcessarExclusao(Lancamento lancamento);
    protected abstract void reverterImpactoFinanceiro(Lancamento lancamentoAntigo);
    protected abstract Lancamento atualizarESalvarLancamento(Lancamento lancamento);
    protected abstract void posProcessarAtualizacao(Lancamento salvo, LancamentoUpdateDTO dto);

    protected void preencherEntidadesComuns(Lancamento lancamento, LancamentoRequestDTO dto) {
        // Validação de recorrência e parcelamento (exclusiva da criação em lote)
        if (dto.tipoRecorrencia() == TipoRecorrencia.PARCELADO) {
            if (dto.quantidadeParcelas() == null || dto.quantidadeParcelas() <= 1) {
                throw new RegraNegocioException("Para lançamentos parcelados, a quantidade de parcelas deve ser maior que 1.");
            }
            if (dto.tipoValorParcelamento() == null) {
                throw new RegraNegocioException("Para lançamentos parcelados, informe se o valor corresponde ao VALOR_TOTAL ou VALOR_PARCELA.");
            }
        } else {
            if (dto.quantidadeParcelas() != null || dto.tipoValorParcelamento() != null) {
                throw new RegraNegocioException("A quantidade de parcelas e o tipo de valor do parcelamento só devem ser informados para lançamentos PARCELADOS.");
            }
        }

        preencherEntidadesBase(
                lancamento,
                dto.tipo(),
                dto.status(),
                dto.dataLancamento(),
                dto.dataEfetivacao(),
                dto.contaId(),
                dto.contaDestinoId(),
                dto.categoriaId(),
                dto.subcategoriaId()
        );
    }

    protected void preencherEntidadesComunsAtualizacao(Lancamento lancamento, LancamentoUpdateDTO dto) {
        preencherEntidadesBase(
                lancamento,
                dto.tipo(),
                dto.status(),
                dto.dataLancamento(),
                dto.dataEfetivacao(),
                dto.contaId(),
                dto.contaDestinoId(),
                dto.categoriaId(),
                dto.subcategoriaId()
        );
    }

    private void preencherEntidadesBase(
            Lancamento lancamento,
            TipoLancamento tipo,
            StatusLancamento status,
            LocalDate dataLancamento,
            LocalDate dataEfetivacao,
            Long contaId,
            Long contaDestinoId,
            Long categoriaId,
            Long subcategoriaId) {

        // 1. Coerência entre status e dataEfetivacao
        if (status == StatusLancamento.EFETIVADO) {
            lancamento.setDataEfetivacao(dataEfetivacao != null ? dataEfetivacao : dataLancamento);
        } else {
            lancamento.setDataEfetivacao(null);
        }

        // 2. Conta de origem
        if (contaId != null) {
            Conta conta = contaRepository.findById(contaId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));
            if (!conta.isAtivo()) {
                throw new RegraNegocioException("Não é possível realizar lançamentos em uma conta inativa.");
            }
            lancamento.setConta(conta);
        } else {
            lancamento.setConta(null);
        }

        // 3. Coerência de Conta de destino conforme o tipo de lançamento
        if (tipo == TipoLancamento.TRANSFERENCIA) {
            if (contaDestinoId == null) {
                throw new RegraNegocioException("A conta de destino é obrigatória para transferências.");
            }
            if (contaDestinoId.equals(contaId)) {
                throw new RegraNegocioException("A conta de destino deve ser diferente da conta de origem.");
            }
            Conta contaDestino = contaRepository.findById(contaDestinoId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta de destino não encontrada"));
            if (!contaDestino.isAtivo()) {
                throw new RegraNegocioException("Não é possível realizar transferências para uma conta inativa.");
            }
            lancamento.setContaDestino(contaDestino);
        } else {
            if (contaDestinoId != null) {
                throw new RegraNegocioException("A conta de destino só deve ser informada em lançamentos do tipo TRANSFERENCIA.");
            }
            lancamento.setContaDestino(null);
        }

        // 4. Categoria e Subcategoria
        if (categoriaId != null) {
            Categoria categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));
            if (!categoria.isAtivo()) {
                throw new RegraNegocioException("Não é possível realizar lançamentos em uma categoria inativa.");
            }
            lancamento.setCategoria(categoria);
        } else {
            throw new RegraNegocioException("Categoria é obrigatória.");
        }

        if (subcategoriaId != null) {
            Subcategoria sub = subcategoriaRepository.findById(subcategoriaId)
                    .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada"));

            if (!sub.isAtivo()) {
                throw new RegraNegocioException("Não é possível realizar lançamentos em uma subcategoria inativa.");
            }

            if (!sub.getCategoria().getId().equals(categoriaId)) {
                throw new RegraNegocioException("A Subcategoria informada não pertence à Categoria selecionada.");
            }
            lancamento.setSubcategoria(sub);
        } else {
            lancamento.setSubcategoria(null);
        }
    }

    protected abstract void preencherEntidadesEspecificas(Lancamento lancamento, LancamentoRequestDTO dto);
    protected abstract void preencherEntidadesEspecificasAtualizacao(Lancamento lancamento, LancamentoUpdateDTO dto);
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
        copia.setTotalParcelas(base.getTotalParcelas());
        copia.setGrupoParcelamentoId(base.getGrupoParcelamentoId());
        copia.setFaturaId(base.getFaturaId());
        copia.setCartaoCreditoId(base.getCartaoCreditoId());
        copia.setDocumentoFiscalId(base.getDocumentoFiscalId());
        return copia;
    }
}
