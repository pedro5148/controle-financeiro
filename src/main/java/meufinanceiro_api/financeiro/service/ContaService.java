package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.faturamento.repository.CartaoCreditoRepository;
import meufinanceiro_api.faturamento.repository.FaturaRepository;
import meufinanceiro_api.faturamento.repository.PagamentoFaturaRepository;
import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.domain.Subcategoria;
import meufinanceiro_api.financeiro.dto.ContaRequestDTO;
import meufinanceiro_api.financeiro.dto.ContaResponseDTO;
import meufinanceiro_api.financeiro.dto.ContaResumoDTO;
import meufinanceiro_api.financeiro.dto.ReajusteSaldoRequestDTO;
import meufinanceiro_api.financeiro.dto.SaldoMensalResponseDTO;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.enums.TipoRecorrencia;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.ContaMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.SubcategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContaService {

    private static final String CATEGORIA_PADRAO_AJUSTE = "Ajuste de Saldo";
    private static final String DESCRICAO_PADRAO_AJUSTE = "Reajuste de saldo";

    private final ContaRepository contaRepository;
    private final ContaMapper contaMapper;
    private final LancamentoRepository lancamentoRepository;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final FaturaRepository faturaRepository;
    private final PagamentoFaturaRepository pagamentoFaturaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;

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

    @Transactional(readOnly = true)
    public SaldoMensalResponseDTO obterSaldosMensais(Integer mes, Integer ano, Long contaId) {
        LocalDate hoje = LocalDate.now();
        int mesFiltro = (mes != null) ? mes : hoje.getMonthValue();
        int anoFiltro = (ano != null) ? ano : hoje.getYear();

        if (mesFiltro < 1 || mesFiltro > 12) {
            throw new RegraNegocioException("O mês informado deve estar entre 1 e 12.");
        }

        BigDecimal saldoAtual;
        if (contaId != null) {
            Conta conta = contaRepository.findById(contaId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));
            saldoAtual = conta.getSaldoAtual() != null ? conta.getSaldoAtual() : BigDecimal.ZERO;
        } else {
            BigDecimal somaAtivas = contaRepository.somarSaldoTotalContasAtivas();
            saldoAtual = somaAtivas != null ? somaAtivas : BigDecimal.ZERO;
        }

        YearMonth competencia = YearMonth.of(anoFiltro, mesFiltro);
        LocalDate inicioMes = competencia.atDay(1);
        LocalDate fimMesAnterior = inicioMes.minusDays(1);
        LocalDate fimMes = competencia.atEndOfMonth();
        LocalDate aposFimMes = fimMes.plusDays(1);

        BigDecimal impactoEfetivadoAPartirDoInicio = calcularImpactoEfetivadoAPartirDe(inicioMes, contaId);
        BigDecimal impactoPendenteAteMesAnterior = calcularImpactoPendenteAte(fimMesAnterior, contaId);
        BigDecimal saldoInicialMes = saldoAtual
                .subtract(impactoEfetivadoAPartirDoInicio)
                .add(impactoPendenteAteMesAnterior);

        BigDecimal impactoEfetivadoAposFimDoMes = calcularImpactoEfetivadoAPartirDe(aposFimMes, contaId);
        BigDecimal impactoPendenteAteFimDoMes = calcularImpactoPendenteAte(fimMes, contaId);
        BigDecimal saldoPrevistoFimMes = saldoAtual
                .subtract(impactoEfetivadoAposFimDoMes)
                .add(impactoPendenteAteFimDoMes);

        return new SaldoMensalResponseDTO(saldoInicialMes, saldoAtual, saldoPrevistoFimMes);
    }

    private BigDecimal calcularImpactoEfetivadoAPartirDe(LocalDate dataCorte, Long contaId) {
        BigDecimal entradas = lancamentoRepository.somarEntradasEfetivadasAPartirDe(dataCorte, contaId);
        BigDecimal saidas = lancamentoRepository.somarSaidasEfetivadasAPartirDe(dataCorte, contaId);
        BigDecimal pagamentosFatura = pagamentoFaturaRepository.somarPagamentosAPartirDe(dataCorte, contaId);

        return (entradas != null ? entradas : BigDecimal.ZERO)
                .subtract(saidas != null ? saidas : BigDecimal.ZERO)
                .subtract(pagamentosFatura != null ? pagamentosFatura : BigDecimal.ZERO);
    }

    private BigDecimal calcularImpactoPendenteAte(LocalDate dataFim, Long contaId) {
        BigDecimal entradasPendentes = lancamentoRepository.somarEntradasPendentesAte(dataFim, contaId);
        BigDecimal saidasPendentes = lancamentoRepository.somarSaidasPendentesAte(dataFim, contaId);
        BigDecimal faturasPendentes = faturaRepository.somarFaturasPendentesAte(dataFim, contaId);

        return (entradasPendentes != null ? entradasPendentes : BigDecimal.ZERO)
                .subtract(saidasPendentes != null ? saidasPendentes : BigDecimal.ZERO)
                .subtract(faturasPendentes != null ? faturasPendentes : BigDecimal.ZERO);
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
            existente.setSaldoInicial(dto.saldoInicial());
        }

        BigDecimal saldoInicialBase = existente.getSaldoInicial() != null ? existente.getSaldoInicial() : BigDecimal.ZERO;
        existente.setSaldoAtual(calcularSaldoRealDaConta(id, saldoInicialBase));

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
    public ContaResponseDTO reajustarSaldoAtual(Long id, ReajusteSaldoRequestDTO dto) {
        Conta existente = contaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));

        if (!existente.isAtivo()) {
            throw new RegraNegocioException("Não é possível reajustar o saldo de uma conta inativa.");
        }

        BigDecimal saldoInicialBase = existente.getSaldoInicial() != null ? existente.getSaldoInicial() : BigDecimal.ZERO;
        BigDecimal saldoAtualReal = calcularSaldoRealDaConta(id, saldoInicialBase);
        BigDecimal diferenca = dto.novoSaldoAtual().subtract(saldoAtualReal);

        if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
            Categoria categoria = obterOuCriarCategoriaAjuste(dto.categoriaId());
            Subcategoria subcategoria = obterSubcategoriaAjuste(dto.subcategoriaId(), categoria.getId());

            String descricao = (dto.descricao() != null && !dto.descricao().isBlank())
                    ? dto.descricao().trim()
                    : DESCRICAO_PADRAO_AJUSTE;

            LocalDate hoje = LocalDate.now();

            Lancamento lancamentoAjuste = new Lancamento();
            lancamentoAjuste.setTipo(diferenca.compareTo(BigDecimal.ZERO) > 0 ? TipoLancamento.RECEITA : TipoLancamento.DESPESA);
            lancamentoAjuste.setDescricao(descricao);
            lancamentoAjuste.setValor(diferenca.abs());
            lancamentoAjuste.setConta(existente);
            lancamentoAjuste.setCategoria(categoria);
            lancamentoAjuste.setSubcategoria(subcategoria);
            lancamentoAjuste.setDataLancamento(hoje);
            lancamentoAjuste.setDataVencimento(hoje);
            lancamentoAjuste.setDataEfetivacao(hoje);
            lancamentoAjuste.setStatus(StatusLancamento.EFETIVADO);
            lancamentoAjuste.setTipoRecorrencia(TipoRecorrencia.NENHUMA);
            lancamentoAjuste.setParcelaAtual(1);
            lancamentoAjuste.setTotalParcelas(1);
            lancamentoAjuste.setObservacoes(dto.observacoes());

            lancamentoRepository.save(lancamentoAjuste);
        }

        existente.setSaldoAtual(dto.novoSaldoAtual());
        return contaMapper.toResponseDTO(contaRepository.save(existente));
    }

    private BigDecimal calcularSaldoRealDaConta(Long contaId, BigDecimal saldoInicial) {
        BigDecimal base = saldoInicial != null ? saldoInicial : BigDecimal.ZERO;
        BigDecimal entradas = lancamentoRepository.somarEntradasEfetivadasPorConta(contaId);
        BigDecimal saidas = lancamentoRepository.somarSaidasEfetivadasPorConta(contaId);
        BigDecimal pagamentosFatura = pagamentoFaturaRepository.somarTotalPagoPorConta(contaId);

        return base
                .add(entradas != null ? entradas : BigDecimal.ZERO)
                .subtract(saidas != null ? saidas : BigDecimal.ZERO)
                .subtract(pagamentosFatura != null ? pagamentosFatura : BigDecimal.ZERO);
    }

    private Categoria obterOuCriarCategoriaAjuste(Long categoriaId) {
        if (categoriaId != null) {
            Categoria categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
            if (!categoria.isAtivo()) {
                throw new RegraNegocioException("Não é possível utilizar uma categoria inativa para o reajuste de saldo.");
            }
            return categoria;
        }

        return categoriaRepository.findByNomeIgnoreCase(CATEGORIA_PADRAO_AJUSTE)
                .map(cat -> {
                    if (!cat.isAtivo()) {
                        cat.setAtivo(true);
                        return categoriaRepository.save(cat);
                    }
                    return cat;
                })
                .orElseGet(() -> {
                    Categoria nova = new Categoria();
                    nova.setNome(CATEGORIA_PADRAO_AJUSTE);
                    nova.setAtivo(true);
                    return categoriaRepository.save(nova);
                });
    }

    private Subcategoria obterSubcategoriaAjuste(Long subcategoriaId, Long categoriaId) {
        if (subcategoriaId == null) {
            return null;
        }
        Subcategoria sub = subcategoriaRepository.findById(subcategoriaId)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada."));
        if (!sub.isAtivo()) {
            throw new RegraNegocioException("Não é possível utilizar uma subcategoria inativa para o reajuste de saldo.");
        }
        if (!sub.getCategoria().getId().equals(categoriaId)) {
            throw new RegraNegocioException("A Subcategoria informada não pertence à Categoria selecionada.");
        }
        return sub;
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

        if (pagamentoFaturaRepository.existsByContaId(id)) {
            throw new RegraNegocioException("Não é possível excluir a conta, pois existem pagamentos de faturas vinculados a ela.");
        }

        // Regra de Negócio
        if (existente.isPadrao()) {
            throw new RegraNegocioException("Não é possível excluir a conta padrão. Defina outra conta como padrão antes de excluí-la.");
        }

        contaRepository.delete(existente);
    }
}