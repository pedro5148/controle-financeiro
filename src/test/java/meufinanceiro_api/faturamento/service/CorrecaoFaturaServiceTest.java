package meufinanceiro_api.faturamento.service;

import meufinanceiro_api.faturamento.domain.CartaoCredito;
import meufinanceiro_api.faturamento.domain.CorrecaoFatura;
import meufinanceiro_api.faturamento.domain.Fatura;
import meufinanceiro_api.faturamento.domain.PagamentoFatura;
import meufinanceiro_api.faturamento.dto.CorrecaoFaturaResponseDTO;
import meufinanceiro_api.faturamento.dto.CorrigirFaturaPagaRequestDTO;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import meufinanceiro_api.faturamento.mapper.CorrecaoFaturaMapper;
import meufinanceiro_api.faturamento.mapper.FaturaMapper;
import meufinanceiro_api.faturamento.mapper.PagamentoFaturaMapper;
import meufinanceiro_api.faturamento.repository.CorrecaoFaturaRepository;
import meufinanceiro_api.faturamento.repository.FaturaRepository;
import meufinanceiro_api.faturamento.repository.PagamentoFaturaRepository;
import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.exception.ConflitoNegocioException;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.LancamentoMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.SubcategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CorrecaoFaturaServiceTest {

    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final PagamentoFaturaRepository pagamentoFaturaRepository = mock(PagamentoFaturaRepository.class);
    private final CorrecaoFaturaRepository correcaoFaturaRepository = mock(CorrecaoFaturaRepository.class);
    private final ContaRepository contaRepository = mock(ContaRepository.class);
    private final CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
    private final SubcategoriaRepository subcategoriaRepository = mock(SubcategoriaRepository.class);
    private final LancamentoRepository lancamentoRepository = mock(LancamentoRepository.class);
    private final FaturaMapper faturaMapper = mock(FaturaMapper.class);
    private final PagamentoFaturaMapper pagamentoFaturaMapper = mock(PagamentoFaturaMapper.class);
    private final LancamentoMapper lancamentoMapper = mock(LancamentoMapper.class);
    private final CorrecaoFaturaMapper correcaoFaturaMapper = mock(CorrecaoFaturaMapper.class);

    private final CorrecaoFaturaService service = new CorrecaoFaturaService(
            faturaRepository,
            pagamentoFaturaRepository,
            correcaoFaturaRepository,
            contaRepository,
            categoriaRepository,
            subcategoriaRepository,
            lancamentoRepository,
            faturaMapper,
            pagamentoFaturaMapper,
            lancamentoMapper,
            correcaoFaturaMapper
    );

    private CartaoCredito cartao;
    private Conta contaOriginal;
    private Conta contaAlternativa;
    private Categoria categoria;
    private Fatura faturaPaga;
    private PagamentoFatura pagamentoOriginal;
    private Lancamento compraOriginal;
    private Lancamento lancamentoPagamentoOriginal;
    private List<Lancamento> lancamentosDaFatura;
    private List<PagamentoFatura> pagamentosDaFatura;

    @BeforeEach
    void setUp() {
        contaOriginal = new Conta();
        contaOriginal.setId(1L);
        contaOriginal.setNome("Nubank");
        contaOriginal.setAtivo(true);
        contaOriginal.setSaldoAtual(new BigDecimal("1000.00"));

        contaAlternativa = new Conta();
        contaAlternativa.setId(2L);
        contaAlternativa.setNome("Itaú");
        contaAlternativa.setAtivo(true);
        contaAlternativa.setSaldoAtual(new BigDecimal("2000.00"));

        cartao = new CartaoCredito();
        cartao.setId(10L);
        cartao.setNome("Cartão Principal");
        cartao.setLimiteTotal(new BigDecimal("5000.00"));
        cartao.setContaPadraoId(1L);
        cartao.setAtivo(true);

        categoria = new Categoria();
        categoria.setId(5L);
        categoria.setNome("Farmácia");
        categoria.setAtivo(true);

        faturaPaga = new Fatura();
        faturaPaga.setId(100L);
        faturaPaga.setCartao(cartao);
        faturaPaga.setMesAno("10/2026");
        faturaPaga.setValorTotal(new BigDecimal("831.98"));
        faturaPaga.setValorPago(new BigDecimal("831.98"));
        faturaPaga.setStatus(StatusFatura.PAGA);
        faturaPaga.setDataFechamento(LocalDate.of(2026, 10, 1));
        faturaPaga.setDataVencimento(LocalDate.of(2026, 10, 10));
        faturaPaga.setVersao(3L);

        LocalDate dataPagamentoOriginal = LocalDate.of(2026, 10, 10);

        compraOriginal = new Lancamento();
        compraOriginal.setId(201L);
        compraOriginal.setTipo(TipoLancamento.DESPESA);
        compraOriginal.setDescricao("Supermercado");
        compraOriginal.setValor(new BigDecimal("831.98"));
        compraOriginal.setStatus(StatusLancamento.EFETIVADO);
        compraOriginal.setDataLancamento(LocalDate.of(2026, 9, 20));
        compraOriginal.setDataVencimento(faturaPaga.getDataVencimento());
        compraOriginal.setDataEfetivacao(dataPagamentoOriginal);
        compraOriginal.setCartaoCreditoId(10L);
        compraOriginal.setFaturaId(100L);

        pagamentoOriginal = new PagamentoFatura();
        pagamentoOriginal.setId(50L);
        pagamentoOriginal.setFatura(faturaPaga);
        pagamentoOriginal.setConta(contaOriginal);
        pagamentoOriginal.setValor(new BigDecimal("831.98"));
        pagamentoOriginal.setDataPagamento(dataPagamentoOriginal);
        pagamentoOriginal.setObservacoes("Pagamento original");

        lancamentoPagamentoOriginal = new Lancamento();
        lancamentoPagamentoOriginal.setId(301L);
        lancamentoPagamentoOriginal.setTipo(TipoLancamento.DESPESA);
        lancamentoPagamentoOriginal.setDescricao("Pagamento fatura 10/2026 - Cartão Principal");
        lancamentoPagamentoOriginal.setValor(new BigDecimal("831.98"));
        lancamentoPagamentoOriginal.setConta(contaOriginal);
        lancamentoPagamentoOriginal.setDataLancamento(dataPagamentoOriginal);
        lancamentoPagamentoOriginal.setDataVencimento(dataPagamentoOriginal);
        lancamentoPagamentoOriginal.setDataEfetivacao(dataPagamentoOriginal);
        lancamentoPagamentoOriginal.setStatus(StatusLancamento.EFETIVADO);
        lancamentoPagamentoOriginal.setPagamentoFaturaId(50L);

        lancamentosDaFatura = new ArrayList<>(List.of(compraOriginal));
        pagamentosDaFatura = new ArrayList<>(List.of(pagamentoOriginal));

        when(correcaoFaturaRepository.existsByChaveIdempotencia(any())).thenReturn(false);
        when(faturaRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(faturaPaga));
        when(faturaRepository.saveAndFlush(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));
        when(contaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(contaOriginal));
        when(contaRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(contaAlternativa));
        when(contaRepository.save(any(Conta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pagamentoFaturaRepository.findByIdAndFaturaIdForUpdate(50L, 100L))
                .thenReturn(Optional.of(pagamentoOriginal));
        when(pagamentoFaturaRepository.saveAndFlush(any(PagamentoFatura.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(pagamentoFaturaRepository.findFirstByFaturaIdOrderByDataPagamentoDescIdDesc(100L))
                .thenAnswer(inv -> Optional.of(pagamentoOriginal));
        when(pagamentoFaturaRepository.somarValorPorFaturaId(100L)).thenAnswer(inv ->
                pagamentosDaFatura.stream()
                        .map(PagamentoFatura::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        when(lancamentoRepository.saveAndFlush(any(Lancamento.class))).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(202L);
            }
            if (!lancamentosDaFatura.contains(l)) {
                lancamentosDaFatura.add(l);
            }
            return l;
        });
        when(lancamentoRepository.findAllByFaturaId(100L)).thenAnswer(inv -> lancamentosDaFatura);
        when(lancamentoRepository.somarValorPorFaturaId(100L)).thenAnswer(inv ->
                lancamentosDaFatura.stream()
                        .map(Lancamento::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        when(lancamentoRepository.findByPagamentoFaturaId(50L))
                .thenReturn(Optional.of(lancamentoPagamentoOriginal));
        when(faturaRepository.somarLimiteUtilizadoPorCartao(10L)).thenAnswer(inv ->
                faturaPaga.getStatus() == StatusFatura.PAGA ? BigDecimal.ZERO : faturaPaga.getSaldoRestante());
        when(correcaoFaturaRepository.saveAndFlush(any(CorrecaoFatura.class))).thenAnswer(inv -> {
            CorrecaoFatura c = inv.getArgument(0);
            c.setId(900L);
            return c;
        });
    }

    @Test
    void deveCorrigirFaturaPagaQuandoPagamentoRealJaIncluiaDespesaEsquecida() {
        LocalDate dataCompraEsquecida = LocalDate.of(2026, 9, 28);
        LocalDate dataPagamentoOriginal = LocalDate.of(2026, 10, 10);

        CorrigirFaturaPagaRequestDTO request = new CorrigirFaturaPagaRequestDTO(
                "corr-100-cenario-1",
                3L,
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Compra farmácia esquecida",
                        new BigDecimal("19.80"),
                        dataCompraEsquecida,
                        5L,
                        null,
                        "Nota localizada depois",
                        null
                ),
                new CorrigirFaturaPagaRequestDTO.CorrecaoPagamentoDTO(
                        50L,
                        1L,
                        dataPagamentoOriginal,
                        new BigDecimal("851.78"),
                        null
                ),
                "Despesa já constava no valor debitado da fatura"
        );

        CorrecaoFaturaResponseDTO response = service.corrigirFaturaPaga(100L, request, null);

        // Fatura recalculada e continua PAGA
        assertEquals(new BigDecimal("851.78"), faturaPaga.getValorTotal());
        assertEquals(new BigDecimal("851.78"), faturaPaga.getValorPago());
        assertEquals(0, faturaPaga.getSaldoRestante().compareTo(BigDecimal.ZERO));
        assertEquals(StatusFatura.PAGA, faturaPaga.getStatus());

        // Pagamento corrigido preservando conta e data original
        assertEquals(new BigDecimal("851.78"), pagamentoOriginal.getValor());
        assertEquals(contaOriginal, pagamentoOriginal.getConta());
        assertEquals(dataPagamentoOriginal, pagamentoOriginal.getDataPagamento());

        // Conta debitada apenas pela diferença (19,80)
        assertEquals(new BigDecimal("980.20"), contaOriginal.getSaldoAtual());

        // Lançamento do pagamento sincronizado
        assertEquals(new BigDecimal("851.78"), lancamentoPagamentoOriginal.getValor());
        assertEquals(contaOriginal, lancamentoPagamentoOriginal.getConta());
        assertEquals(dataPagamentoOriginal, lancamentoPagamentoOriginal.getDataEfetivacao());

        // Nova despesa vinculada diretamente à fatura original e efetivada na data do pagamento
        assertEquals(2, lancamentosDaFatura.size());
        Lancamento novaDespesa = lancamentosDaFatura.get(1);
        assertEquals(100L, novaDespesa.getFaturaId());
        assertEquals(dataCompraEsquecida, novaDespesa.getDataLancamento());
        assertEquals(faturaPaga.getDataVencimento(), novaDespesa.getDataVencimento());
        assertEquals(StatusLancamento.EFETIVADO, novaDespesa.getStatus());
        assertEquals(dataPagamentoOriginal, novaDespesa.getDataEfetivacao());
        assertEquals(StatusLancamento.EFETIVADO, compraOriginal.getStatus());

        // Limite disponível do cartão continua integral (fatura quitada)
        assertEquals(0, response.limiteUtilizadoCartao().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("5000.00"), response.limiteDisponivelCartao());

        // Histórico salvo corretamente
        ArgumentCaptor<CorrecaoFatura> captor = ArgumentCaptor.forClass(CorrecaoFatura.class);
        verify(correcaoFaturaRepository).saveAndFlush(captor.capture());
        CorrecaoFatura historico = captor.getValue();
        assertEquals("corr-100-cenario-1", historico.getChaveIdempotencia());
        assertTrue(historico.isPagamentoCorrigido());
        assertEquals(new BigDecimal("831.98"), historico.getValorTotalAnterior());
        assertEquals(new BigDecimal("851.78"), historico.getValorTotalNovo());
        assertEquals(StatusFatura.PAGA, historico.getStatusAnterior());
        assertEquals(StatusFatura.PAGA, historico.getStatusNovo());
    }

    @Test
    void deveIncluirDespesaEDeixarDiferencaPendenteQuandoPagamentoRealFoiMenorQueNovoTotal() {
        LocalDate dataCompraEsquecida = LocalDate.of(2026, 9, 28);

        CorrigirFaturaPagaRequestDTO request = new CorrigirFaturaPagaRequestDTO(
                null,
                3L,
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Compra farmácia esquecida",
                        new BigDecimal("19.80"),
                        dataCompraEsquecida,
                        5L,
                        null,
                        null,
                        null
                ),
                null,
                "Pagamento real foi de R$ 831,98; faltam R$ 19,80"
        );

        CorrecaoFaturaResponseDTO response = service.corrigirFaturaPaga(100L, request, "corr-100-cenario-2");

        // Fatura recalculada e volta para FECHADA com R$ 19,80 pendentes
        assertEquals(new BigDecimal("851.78"), faturaPaga.getValorTotal());
        assertEquals(new BigDecimal("831.98"), faturaPaga.getValorPago());
        assertEquals(new BigDecimal("19.80"), faturaPaga.getSaldoRestante());
        assertEquals(StatusFatura.FECHADA, faturaPaga.getStatus());

        // Nenhum pagamento criado ou alterado e saldo da conta preservado
        verify(pagamentoFaturaRepository, never()).save(any());
        verify(pagamentoFaturaRepository, never()).saveAndFlush(any());
        assertEquals(new BigDecimal("1000.00"), contaOriginal.getSaldoAtual());

        // Todos os lançamentos da fatura (antigo e novo) voltam para PENDENTE
        assertEquals(2, lancamentosDaFatura.size());
        Lancamento novaDespesa = lancamentosDaFatura.get(1);
        assertEquals(100L, novaDespesa.getFaturaId());
        assertEquals(dataCompraEsquecida, novaDespesa.getDataLancamento());
        assertEquals(StatusLancamento.PENDENTE, novaDespesa.getStatus());
        assertNull(novaDespesa.getDataEfetivacao());
        assertEquals(StatusLancamento.PENDENTE, compraOriginal.getStatus());
        assertNull(compraOriginal.getDataEfetivacao());

        // Limite disponível do cartão reduzido pela diferença pendente (19,80)
        assertEquals(new BigDecimal("19.80"), response.limiteUtilizadoCartao());
        assertEquals(new BigDecimal("4980.20"), response.limiteDisponivelCartao());

        // Histórico salvo com pagamentoCorrigido = false
        ArgumentCaptor<CorrecaoFatura> captor = ArgumentCaptor.forClass(CorrecaoFatura.class);
        verify(correcaoFaturaRepository).saveAndFlush(captor.capture());
        CorrecaoFatura historico = captor.getValue();
        assertFalse(historico.isPagamentoCorrigido());
        assertNull(historico.getPagamentoFaturaId());
        assertEquals(StatusFatura.PAGA, historico.getStatusAnterior());
        assertEquals(StatusFatura.FECHADA, historico.getStatusNovo());
        assertEquals(new BigDecimal("19.80"), historico.getSaldoRestanteNovo());
    }

    @Test
    void deveAjustarDuasContasEDataRealQuandoCorrecaoPagamentoTrocaContaEDataComNovoValorExplicito() {
        LocalDate novaDataPagamento = LocalDate.of(2026, 10, 12);

        CorrigirFaturaPagaRequestDTO request = new CorrigirFaturaPagaRequestDTO(
                "corr-100-troca-conta",
                null,
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Farmácia",
                        new BigDecimal("19.80"),
                        LocalDate.of(2026, 9, 28),
                        5L,
                        null,
                        null,
                        null
                ),
                new CorrigirFaturaPagaRequestDTO.CorrecaoPagamentoDTO(
                        50L,
                        2L,
                        novaDataPagamento,
                        new BigDecimal("851.78"),
                        "Pago na conta Itaú"
                ),
                null
        );

        service.corrigirFaturaPaga(100L, request, null);

        // Conta original estornada em 831,98; conta alternativa debitada em 851,78
        assertEquals(new BigDecimal("1831.98"), contaOriginal.getSaldoAtual());
        assertEquals(new BigDecimal("1148.22"), contaAlternativa.getSaldoAtual());
        assertEquals(contaAlternativa, pagamentoOriginal.getConta());
        assertEquals(new BigDecimal("851.78"), pagamentoOriginal.getValor());
        assertEquals(novaDataPagamento, pagamentoOriginal.getDataPagamento());
        assertEquals(novaDataPagamento, compraOriginal.getDataEfetivacao());
        assertEquals(StatusFatura.PAGA, faturaPaga.getStatus());
    }

    @Test
    void deveManterValorAtualDoPagamentoEDeixarDiferencaPendenteQuandoNovoValorForOmitido() {
        LocalDate novaDataPagamento = LocalDate.of(2026, 10, 12);

        CorrigirFaturaPagaRequestDTO request = new CorrigirFaturaPagaRequestDTO(
                "corr-100-sem-novo-valor",
                null,
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Farmácia",
                        new BigDecimal("19.80"),
                        LocalDate.of(2026, 9, 28),
                        5L,
                        null,
                        null,
                        null
                ),
                new CorrigirFaturaPagaRequestDTO.CorrecaoPagamentoDTO(
                        50L,
                        2L,
                        novaDataPagamento,
                        null,
                        "Apenas corrigindo conta e data do pagamento"
                ),
                null
        );

        CorrecaoFaturaResponseDTO response = service.corrigirFaturaPaga(100L, request, null);

        // Valor pago NÃO é aumentado automaticamente; apenas conta e data são corrigidas
        assertEquals(new BigDecimal("831.98"), pagamentoOriginal.getValor());
        assertEquals(contaAlternativa, pagamentoOriginal.getConta());
        assertEquals(novaDataPagamento, pagamentoOriginal.getDataPagamento());

        // Estorna 831,98 da conta original e debita exatamente 831,98 na nova conta
        assertEquals(new BigDecimal("1831.98"), contaOriginal.getSaldoAtual());
        assertEquals(new BigDecimal("1168.02"), contaAlternativa.getSaldoAtual());

        // Diferença de 19,80 fica pendente
        assertEquals(new BigDecimal("851.78"), faturaPaga.getValorTotal());
        assertEquals(new BigDecimal("831.98"), faturaPaga.getValorPago());
        assertEquals(new BigDecimal("19.80"), faturaPaga.getSaldoRestante());
        assertEquals(StatusFatura.FECHADA, faturaPaga.getStatus());
        assertEquals(StatusLancamento.PENDENTE, compraOriginal.getStatus());
        assertNull(compraOriginal.getDataEfetivacao());
        assertEquals(new BigDecimal("19.80"), response.limiteUtilizadoCartao());
        assertEquals(new BigDecimal("4980.20"), response.limiteDisponivelCartao());
    }

    @Test
    void deveRejeitarValoresComMaisDeDuasCasasDecimais() {
        CorrigirFaturaPagaRequestDTO requestDespesaInvalida = new CorrigirFaturaPagaRequestDTO(
                "chave-decimal-despesa",
                null,
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Farmácia",
                        new BigDecimal("0.001"),
                        LocalDate.of(2026, 9, 28),
                        5L,
                        null,
                        null,
                        null
                ),
                null,
                null
        );

        assertThrows(RegraNegocioException.class,
                () -> service.corrigirFaturaPaga(100L, requestDespesaInvalida, null));
        verify(lancamentoRepository, never()).saveAndFlush(any());

        CorrigirFaturaPagaRequestDTO requestPagamentoInvalido = new CorrigirFaturaPagaRequestDTO(
                "chave-decimal-pagamento",
                null,
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Farmácia",
                        new BigDecimal("19.80"),
                        LocalDate.of(2026, 9, 28),
                        5L,
                        null,
                        null,
                        null
                ),
                new CorrigirFaturaPagaRequestDTO.CorrecaoPagamentoDTO(
                        50L,
                        1L,
                        LocalDate.of(2026, 10, 10),
                        new BigDecimal("851.785"),
                        null
                ),
                null
        );

        assertThrows(RegraNegocioException.class,
                () -> service.corrigirFaturaPaga(100L, requestPagamentoInvalido, null));
        verify(correcaoFaturaRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveImpedirProcessamentoDuplicadoComMesmaChaveIdempotencia() {
        when(correcaoFaturaRepository.existsByChaveIdempotencia("chave-repetida")).thenReturn(true);

        CorrigirFaturaPagaRequestDTO request = new CorrigirFaturaPagaRequestDTO(
                "chave-repetida",
                null,
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Farmácia",
                        new BigDecimal("19.80"),
                        LocalDate.of(2026, 9, 28),
                        5L,
                        null,
                        null,
                        null
                ),
                null,
                null
        );

        assertThrows(ConflitoNegocioException.class,
                () -> service.corrigirFaturaPaga(100L, request, null));
        verify(faturaRepository, never()).findByIdForUpdate(any());
        verify(lancamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarQuandoVersaoDaFaturaEstiverDesatualizada() {
        CorrigirFaturaPagaRequestDTO request = new CorrigirFaturaPagaRequestDTO(
                "chave-versao",
                2L, // versão atual é 3L
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Farmácia",
                        new BigDecimal("19.80"),
                        LocalDate.of(2026, 9, 28),
                        5L,
                        null,
                        null,
                        null
                ),
                null,
                null
        );

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> service.corrigirFaturaPaga(100L, request, null));
        verify(lancamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarFaturaQueNaoEstejaComStatusPaga() {
        faturaPaga.setStatus(StatusFatura.FECHADA);

        CorrigirFaturaPagaRequestDTO request = new CorrigirFaturaPagaRequestDTO(
                "chave-fechada",
                null,
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Farmácia",
                        new BigDecimal("19.80"),
                        LocalDate.of(2026, 9, 28),
                        5L,
                        null,
                        null,
                        null
                ),
                null,
                null
        );

        assertThrows(RegraNegocioException.class,
                () -> service.corrigirFaturaPaga(100L, request, null));
        verify(lancamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarQuandoNovoValorPagamentoUltrapassarNovoTotalDaFatura() {
        CorrigirFaturaPagaRequestDTO request = new CorrigirFaturaPagaRequestDTO(
                "chave-excede",
                null,
                new CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO(
                        "Farmácia",
                        new BigDecimal("19.80"),
                        LocalDate.of(2026, 9, 28),
                        5L,
                        null,
                        null,
                        null
                ),
                new CorrigirFaturaPagaRequestDTO.CorrecaoPagamentoDTO(
                        50L,
                        1L,
                        LocalDate.of(2026, 10, 10),
                        new BigDecimal("900.00"),
                        null
                ),
                null
        );

        assertThrows(RegraNegocioException.class,
                () -> service.corrigirFaturaPaga(100L, request, null));
        verify(correcaoFaturaRepository, never()).saveAndFlush(any());
    }
}
