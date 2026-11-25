package meufinanceiro_api.financeiro.service.strategy;

import meufinanceiro_api.faturamento.domain.CartaoCredito;
import meufinanceiro_api.faturamento.domain.Fatura;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import meufinanceiro_api.faturamento.mapper.CartaoCreditoMapper;
import meufinanceiro_api.faturamento.mapper.FaturaMapper;
import meufinanceiro_api.faturamento.mapper.PagamentoFaturaMapper;
import meufinanceiro_api.faturamento.repository.CartaoCreditoRepository;
import meufinanceiro_api.faturamento.repository.FaturaRepository;
import meufinanceiro_api.faturamento.repository.PagamentoFaturaRepository;
import meufinanceiro_api.faturamento.service.CartaoCreditoService;
import meufinanceiro_api.faturamento.service.FaturaService;
import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.domain.Subcategoria;
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
import meufinanceiro_api.financeiro.service.CategoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LancamentoCreditoStrategyTest {

    private final LancamentoRepository lancamentoRepository = mock(LancamentoRepository.class);
    private final ContaRepository contaRepository = mock(ContaRepository.class);
    private final CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
    private final SubcategoriaRepository subcategoriaRepository = mock(SubcategoriaRepository.class);
    private final LancamentoMapper lancamentoMapper = mock(LancamentoMapper.class);

    private final CartaoCreditoRepository cartaoCreditoRepository = mock(CartaoCreditoRepository.class);
    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final PagamentoFaturaRepository pagamentoFaturaRepository = mock(PagamentoFaturaRepository.class);
    private final CategoriaService categoriaService = mock(CategoriaService.class);
    private final FaturaMapper faturaMapper = mock(FaturaMapper.class);
    private final PagamentoFaturaMapper pagamentoFaturaMapper = mock(PagamentoFaturaMapper.class);
    private final CartaoCreditoMapper cartaoCreditoMapper = mock(CartaoCreditoMapper.class);

    private final CartaoCreditoService cartaoCreditoService = new CartaoCreditoService(
            cartaoCreditoRepository,
            faturaRepository,
            contaRepository,
            lancamentoRepository,
            cartaoCreditoMapper
    );

    private final FaturaService faturaService = new FaturaService(
            faturaRepository,
            pagamentoFaturaRepository,
            contaRepository,
            categoriaService,
            lancamentoRepository,
            faturaMapper,
            pagamentoFaturaMapper
    );

    private final LancamentoCreditoStrategy strategy = new LancamentoCreditoStrategy(
            lancamentoRepository,
            contaRepository,
            categoriaRepository,
            subcategoriaRepository,
            lancamentoMapper,
            cartaoCreditoService,
            faturaService
    );

    private CartaoCredito amazonCard;
    private CartaoCredito outroCartao;
    private Categoria vestuario;
    private Subcategoria camisas;
    private Fatura faturaNovembroPaga;
    private Fatura faturaDezembroAberta;
    private Fatura faturaJaneiroAberta;
    private Lancamento parcelaDezembro;

    @BeforeEach
    void setUp() {
        amazonCard = new CartaoCredito();
        amazonCard.setId(10L);
        amazonCard.setNome("AmazonCard");
        amazonCard.setLimiteTotal(new BigDecimal("5000.00"));
        amazonCard.setDiaFechamento(3);
        amazonCard.setDiaVencimento(10);
        amazonCard.setContaPadraoId(1L);
        amazonCard.setAtivo(true);

        outroCartao = new CartaoCredito();
        outroCartao.setId(20L);
        outroCartao.setNome("Nubank");
        outroCartao.setLimiteTotal(new BigDecimal("5000.00"));
        outroCartao.setDiaFechamento(5);
        outroCartao.setDiaVencimento(12);
        outroCartao.setContaPadraoId(1L);
        outroCartao.setAtivo(true);

        vestuario = new Categoria();
        vestuario.setId(5L);
        vestuario.setNome("Vestuário");
        vestuario.setAtivo(true);

        camisas = new Subcategoria();
        camisas.setId(50L);
        camisas.setNome("Camisas");
        camisas.setAtivo(true);
        camisas.setCategoria(vestuario);

        faturaNovembroPaga = new Fatura();
        faturaNovembroPaga.setId(11L);
        faturaNovembroPaga.setCartao(amazonCard);
        faturaNovembroPaga.setMesAno("11/2026");
        faturaNovembroPaga.setValorTotal(new BigDecimal("200.00"));
        faturaNovembroPaga.setValorPago(new BigDecimal("200.00"));
        faturaNovembroPaga.setStatus(StatusFatura.PAGA);
        faturaNovembroPaga.setDataFechamento(LocalDate.of(2026, 11, 3));
        faturaNovembroPaga.setDataVencimento(LocalDate.of(2026, 11, 10));

        faturaDezembroAberta = new Fatura();
        faturaDezembroAberta.setId(12L);
        faturaDezembroAberta.setCartao(amazonCard);
        faturaDezembroAberta.setMesAno("12/2026");
        faturaDezembroAberta.setValorTotal(new BigDecimal("149.00"));
        faturaDezembroAberta.setValorPago(BigDecimal.ZERO);
        faturaDezembroAberta.setStatus(StatusFatura.ABERTA);
        faturaDezembroAberta.setDataFechamento(LocalDate.of(2026, 12, 3));
        faturaDezembroAberta.setDataVencimento(LocalDate.of(2026, 12, 10));

        faturaJaneiroAberta = new Fatura();
        faturaJaneiroAberta.setId(13L);
        faturaJaneiroAberta.setCartao(amazonCard);
        faturaJaneiroAberta.setMesAno("01/2027");
        faturaJaneiroAberta.setValorTotal(new BigDecimal("49.00"));
        faturaJaneiroAberta.setValorPago(BigDecimal.ZERO);
        faturaJaneiroAberta.setStatus(StatusFatura.ABERTA);
        faturaJaneiroAberta.setDataFechamento(LocalDate.of(2027, 1, 3));
        faturaJaneiroAberta.setDataVencimento(LocalDate.of(2027, 1, 10));

        parcelaDezembro = new Lancamento();
        parcelaDezembro.setId(1002L);
        parcelaDezembro.setTipo(TipoLancamento.DESPESA);
        parcelaDezembro.setDescricao("Onebasics");
        parcelaDezembro.setValor(new BigDecimal("49.00"));
        parcelaDezembro.setCategoria(vestuario);
        parcelaDezembro.setSubcategoria(camisas);
        parcelaDezembro.setDataLancamento(LocalDate.of(2026, 10, 6));
        parcelaDezembro.setDataVencimento(LocalDate.of(2026, 12, 10));
        parcelaDezembro.setStatus(StatusLancamento.PENDENTE);
        parcelaDezembro.setTipoRecorrencia(TipoRecorrencia.PARCELADO);
        parcelaDezembro.setParcelaAtual(2);
        parcelaDezembro.setTotalParcelas(3);
        parcelaDezembro.setGrupoParcelamentoId(1001L);
        parcelaDezembro.setCartaoCreditoId(amazonCard.getId());
        parcelaDezembro.setFaturaId(faturaDezembroAberta.getId());

        when(cartaoCreditoRepository.findById(10L)).thenReturn(Optional.of(amazonCard));
        when(cartaoCreditoRepository.findById(20L)).thenReturn(Optional.of(outroCartao));
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(vestuario));
        when(subcategoriaRepository.findById(50L)).thenReturn(Optional.of(camisas));

        when(faturaRepository.findById(11L)).thenReturn(Optional.of(faturaNovembroPaga));
        when(faturaRepository.findById(12L)).thenReturn(Optional.of(faturaDezembroAberta));
        when(faturaRepository.findById(13L)).thenReturn(Optional.of(faturaJaneiroAberta));
        when(faturaRepository.findByCartaoIdAndMesAno(10L, "11/2026")).thenReturn(Optional.of(faturaNovembroPaga));
        when(faturaRepository.findByCartaoIdAndMesAno(10L, "12/2026")).thenReturn(Optional.of(faturaDezembroAberta));
        when(faturaRepository.findByCartaoIdAndMesAno(10L, "01/2027")).thenReturn(Optional.of(faturaJaneiroAberta));
        when(faturaRepository.saveAndFlush(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(faturaRepository.somarLimiteUtilizadoPorCartao(10L)).thenAnswer(inv ->
                faturaDezembroAberta.getSaldoRestante().add(faturaJaneiroAberta.getSaldoRestante()));
        when(faturaRepository.somarLimiteUtilizadoPorCartao(20L)).thenReturn(BigDecimal.ZERO);

        when(lancamentoRepository.save(any(Lancamento.class))).thenAnswer(inv -> inv.getArgument(0));

        // Simula o comportamento real do MapStruct em atualizarEntidade (inclusive sobrescrevendo dataVencimento)
        doAnswer(inv -> {
            LancamentoUpdateDTO dto = inv.getArgument(0);
            Lancamento entity = inv.getArgument(1);
            entity.setTipo(dto.tipo());
            entity.setDescricao(dto.descricao());
            entity.setValor(dto.valor());
            entity.setDataLancamento(dto.dataLancamento());
            entity.setDataVencimento(dto.dataVencimento());
            entity.setDataEfetivacao(dto.dataEfetivacao());
            entity.setStatus(dto.status());
            entity.setObservacoes(dto.observacoes());
            entity.setCartaoCreditoId(dto.cartaoCreditoId());
            entity.setDocumentoFiscalId(dto.documentoFiscalId());
            return null;
        }).when(lancamentoMapper).atualizarEntidade(any(LancamentoUpdateDTO.class), any(Lancamento.class));
    }

    @Test
    void deveManterFaturaDeDezembroEReduzirTotalEmDoisCentavosQuandoFaturaAnteriorJaEstiverPaga() {
        LancamentoUpdateDTO dto = new LancamentoUpdateDTO(
                TipoLancamento.DESPESA,
                "Onebasics",
                new BigDecimal("48.98"),
                null,
                null,
                vestuario.getId(),
                camisas.getId(),
                LocalDate.of(2026, 10, 6),
                null,
                null,
                StatusLancamento.PENDENTE,
                null,
                amazonCard.getId(),
                null
        );

        strategy.atualizar(parcelaDezembro, dto, false);

        assertEquals(faturaDezembroAberta.getId(), parcelaDezembro.getFaturaId());
        assertEquals(LocalDate.of(2026, 12, 10), parcelaDezembro.getDataVencimento());
        assertEquals(new BigDecimal("48.98"), parcelaDezembro.getValor());
        assertEquals(new BigDecimal("148.98"), faturaDezembroAberta.getValorTotal());
        assertEquals(new BigDecimal("49.00"), faturaJaneiroAberta.getValorTotal());
        verify(faturaRepository, never()).findByCartaoIdAndMesAno(anyLong(), anyString());
    }

    @Test
    void deveValidarLimiteApenasPelaDiferencaAoAumentarValorNaMesmaFatura() {
        amazonCard.setLimiteTotal(new BigDecimal("210.00")); // utilizado = 198.00, disponivel = 12.00

        LancamentoUpdateDTO aumentoPermitido = new LancamentoUpdateDTO(
                TipoLancamento.DESPESA,
                "Onebasics",
                new BigDecimal("59.00"), // +10.00 (menor que os 12.00 disponíveis, embora 59.00 > 12.00)
                null,
                null,
                vestuario.getId(),
                camisas.getId(),
                LocalDate.of(2026, 10, 6),
                null,
                null,
                StatusLancamento.PENDENTE,
                "Ajuste de frete",
                amazonCard.getId(),
                null
        );

        strategy.atualizar(parcelaDezembro, aumentoPermitido, false);

        assertEquals(faturaDezembroAberta.getId(), parcelaDezembro.getFaturaId());
        assertEquals(LocalDate.of(2026, 12, 10), parcelaDezembro.getDataVencimento());
        assertEquals(new BigDecimal("159.00"), faturaDezembroAberta.getValorTotal());

        LancamentoUpdateDTO aumentoAcimaDoLimite = new LancamentoUpdateDTO(
                TipoLancamento.DESPESA,
                "Onebasics",
                new BigDecimal("65.00"), // +6.00 (restam apenas 2.00 disponíveis)
                null,
                null,
                vestuario.getId(),
                camisas.getId(),
                LocalDate.of(2026, 10, 6),
                null,
                null,
                StatusLancamento.PENDENTE,
                null,
                amazonCard.getId(),
                null
        );

        assertThrows(RegraNegocioException.class, () -> strategy.atualizar(parcelaDezembro, aumentoAcimaDoLimite, false));
        assertEquals(new BigDecimal("159.00"), faturaDezembroAberta.getValorTotal());
    }

    @Test
    void devePreservarFaturaSemAlterarTotalAoEditarApenasMetadados() {
        LancamentoUpdateDTO dto = new LancamentoUpdateDTO(
                TipoLancamento.DESPESA,
                "Onebasics Camiseta",
                new BigDecimal("49.00"),
                null,
                null,
                vestuario.getId(),
                camisas.getId(),
                LocalDate.of(2026, 10, 6),
                null,
                null,
                StatusLancamento.PENDENTE,
                "Observação atualizada",
                amazonCard.getId(),
                null
        );

        strategy.atualizar(parcelaDezembro, dto, false);

        assertEquals(faturaDezembroAberta.getId(), parcelaDezembro.getFaturaId());
        assertEquals(LocalDate.of(2026, 12, 10), parcelaDezembro.getDataVencimento());
        assertEquals("Onebasics Camiseta", parcelaDezembro.getDescricao());
        assertEquals("Observação atualizada", parcelaDezembro.getObservacoes());
        assertEquals(new BigDecimal("149.00"), faturaDezembroAberta.getValorTotal());
        verify(faturaRepository, never()).saveAndFlush(any(Fatura.class));
        verify(faturaRepository, never()).findByCartaoIdAndMesAno(anyLong(), anyString());
    }

    @Test
    void deveBloquearEdicaoQuandoFaturaOriginalNaoEstiverAberta() {
        faturaDezembroAberta.setStatus(StatusFatura.FECHADA);

        LancamentoUpdateDTO dto = new LancamentoUpdateDTO(
                TipoLancamento.DESPESA,
                "Onebasics",
                new BigDecimal("49.00"),
                null,
                null,
                vestuario.getId(),
                camisas.getId(),
                LocalDate.of(2026, 10, 6),
                null,
                null,
                StatusLancamento.PENDENTE,
                "Tentativa em fatura fechada",
                amazonCard.getId(),
                null
        );

        assertThrows(RegraNegocioException.class, () -> strategy.atualizar(parcelaDezembro, dto, false));
        verify(lancamentoRepository, never()).save(any(Lancamento.class));
    }

    @Test
    void deveRespeitarPagamentosJaRealizadosAoReduzirValorNaMesmaFatura() {
        faturaDezembroAberta.setValorTotal(new BigDecimal("100.00"));
        faturaDezembroAberta.setValorPago(new BigDecimal("80.00"));

        // Redução de R$ 0,02 deixa total em 99,98 (>= 80,00 já pagos): deve permitir
        LancamentoUpdateDTO reducaoValida = new LancamentoUpdateDTO(
                TipoLancamento.DESPESA,
                "Onebasics",
                new BigDecimal("48.98"),
                null,
                null,
                vestuario.getId(),
                camisas.getId(),
                LocalDate.of(2026, 10, 6),
                null,
                null,
                StatusLancamento.PENDENTE,
                null,
                amazonCard.getId(),
                null
        );

        strategy.atualizar(parcelaDezembro, reducaoValida, false);
        assertEquals(new BigDecimal("99.98"), faturaDezembroAberta.getValorTotal());

        // Redução de R$ 28,98 deixaria total em 71,00 (< 80,00 já pagos): deve bloquear
        LancamentoUpdateDTO reducaoInvalida = new LancamentoUpdateDTO(
                TipoLancamento.DESPESA,
                "Onebasics",
                new BigDecimal("20.00"),
                null,
                null,
                vestuario.getId(),
                camisas.getId(),
                LocalDate.of(2026, 10, 6),
                null,
                null,
                StatusLancamento.PENDENTE,
                null,
                amazonCard.getId(),
                null
        );

        assertThrows(RegraNegocioException.class, () -> strategy.atualizar(parcelaDezembro, reducaoInvalida, false));
        assertEquals(new BigDecimal("99.98"), faturaDezembroAberta.getValorTotal());
    }

    @Test
    void deveRecalcularFaturaQuandoDataDaCompraOuCartaoForAlterado() {
        Fatura faturaOutroCartao = new Fatura();
        faturaOutroCartao.setId(21L);
        faturaOutroCartao.setCartao(outroCartao);
        faturaOutroCartao.setMesAno("12/2026");
        faturaOutroCartao.setValorTotal(BigDecimal.ZERO);
        faturaOutroCartao.setValorPago(BigDecimal.ZERO);
        faturaOutroCartao.setStatus(StatusFatura.ABERTA);
        faturaOutroCartao.setDataFechamento(LocalDate.of(2026, 12, 5));
        faturaOutroCartao.setDataVencimento(LocalDate.of(2026, 12, 12));

        when(faturaRepository.findByCartaoIdAndMesAno(20L, "11/2026")).thenReturn(Optional.empty());
        when(faturaRepository.findByCartaoIdAndMesAno(20L, "12/2026")).thenReturn(Optional.of(faturaOutroCartao));
        when(faturaRepository.findById(21L)).thenReturn(Optional.of(faturaOutroCartao));

        LancamentoUpdateDTO trocaCartao = new LancamentoUpdateDTO(
                TipoLancamento.DESPESA,
                "Onebasics",
                new BigDecimal("48.98"),
                null,
                null,
                vestuario.getId(),
                camisas.getId(),
                LocalDate.of(2026, 10, 6),
                null,
                null,
                StatusLancamento.PENDENTE,
                null,
                outroCartao.getId(),
                null
        );

        strategy.atualizar(parcelaDezembro, trocaCartao, false);

        assertEquals(21L, parcelaDezembro.getFaturaId());
        assertEquals(LocalDate.of(2026, 12, 12), parcelaDezembro.getDataVencimento());
        assertEquals(new BigDecimal("100.00"), faturaDezembroAberta.getValorTotal());
        assertEquals(new BigDecimal("48.98"), faturaOutroCartao.getValorTotal());
    }
}
