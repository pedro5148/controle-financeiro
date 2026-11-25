package meufinanceiro_api.faturamento.service;

import meufinanceiro_api.faturamento.domain.CartaoCredito;
import meufinanceiro_api.faturamento.domain.Fatura;
import meufinanceiro_api.faturamento.domain.PagamentoFatura;
import meufinanceiro_api.faturamento.dto.PagamentoFaturaRequestDTO;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import meufinanceiro_api.faturamento.mapper.FaturaMapper;
import meufinanceiro_api.faturamento.mapper.PagamentoFaturaMapper;
import meufinanceiro_api.faturamento.repository.FaturaRepository;
import meufinanceiro_api.faturamento.repository.PagamentoFaturaRepository;
import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.enums.CategoriaSistema;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.service.CategoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FaturaServicePagamentoTest {

    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final PagamentoFaturaRepository pagamentoFaturaRepository = mock(PagamentoFaturaRepository.class);
    private final ContaRepository contaRepository = mock(ContaRepository.class);
    private final CategoriaService categoriaService = mock(CategoriaService.class);
    private final LancamentoRepository lancamentoRepository = mock(LancamentoRepository.class);
    private final FaturaMapper faturaMapper = mock(FaturaMapper.class);
    private final PagamentoFaturaMapper pagamentoFaturaMapper = mock(PagamentoFaturaMapper.class);

    private final FaturaService faturaService = new FaturaService(
            faturaRepository,
            pagamentoFaturaRepository,
            contaRepository,
            categoriaService,
            lancamentoRepository,
            faturaMapper,
            pagamentoFaturaMapper
    );

    private Conta conta;
    private Fatura fatura;
    private Lancamento compra1;
    private Lancamento compra2;

    @BeforeEach
    void setUp() {
        conta = new Conta();
        conta.setId(1L);
        conta.setNome("Mercado Pago");
        conta.setAtivo(true);
        conta.setSaldoAtual(new BigDecimal("1000.00"));

        CartaoCredito cartao = new CartaoCredito();
        cartao.setId(10L);
        cartao.setNome("Mercado Pago");
        cartao.setContaPadraoId(1L);

        fatura = new Fatura();
        fatura.setId(100L);
        fatura.setCartao(cartao);
        fatura.setMesAno("11/2026");
        fatura.setValorTotal(new BigDecimal("500.00"));
        fatura.setValorPago(BigDecimal.ZERO);
        fatura.setStatus(StatusFatura.ABERTA);

        compra1 = new Lancamento();
        compra1.setId(201L);
        compra1.setTipo(TipoLancamento.DESPESA);
        compra1.setValor(new BigDecimal("300.00"));
        compra1.setStatus(StatusLancamento.PENDENTE);
        compra1.setFaturaId(100L);

        compra2 = new Lancamento();
        compra2.setId(202L);
        compra2.setTipo(TipoLancamento.DESPESA);
        compra2.setValor(new BigDecimal("200.00"));
        compra2.setStatus(StatusLancamento.PENDENTE);
        compra2.setFaturaId(100L);

        when(faturaRepository.findById(100L)).thenReturn(Optional.of(fatura));
        when(faturaRepository.saveAndFlush(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contaRepository.findById(1L)).thenReturn(Optional.of(conta));
        when(categoriaService.obterOuCriarCategoriaSistema(CategoriaSistema.PAGAMENTO_FATURA))
                .thenReturn(new Categoria());
        when(pagamentoFaturaRepository.save(any(PagamentoFatura.class))).thenAnswer(inv -> {
            PagamentoFatura p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(999L);
            }
            return p;
        });
        when(lancamentoRepository.findAllByFaturaId(100L)).thenReturn(List.of(compra1, compra2));
    }

    @Test
    void pagamentoParcialComplementarEEstornoAtualizamStatusDasComprasCorretamente() {
        // 1. Pagamento parcial mantém a fatura ABERTA e as compras PENDENTE
        LocalDate dataParcial = LocalDate.of(2026, 10, 26);
        faturaService.pagarFatura(100L, new PagamentoFaturaRequestDTO(
                new BigDecimal("200.00"), 1L, dataParcial, "Parcial"));

        assertEquals(StatusFatura.ABERTA, fatura.getStatus());
        assertEquals(new BigDecimal("300.00"), fatura.getSaldoRestante());
        assertEquals(new BigDecimal("800.00"), conta.getSaldoAtual());
        assertEquals(StatusLancamento.PENDENTE, compra1.getStatus());
        assertNull(compra1.getDataEfetivacao());
        assertEquals(StatusLancamento.PENDENTE, compra2.getStatus());
        assertNull(compra2.getDataEfetivacao());

        // 2. Pagamento complementar que zera o saldo muda a fatura para PAGA e efetiva as compras
        LocalDate dataComplementar = LocalDate.of(2026, 11, 8);
        faturaService.pagarFatura(100L, new PagamentoFaturaRequestDTO(
                new BigDecimal("300.00"), 1L, dataComplementar, "Complementar"));

        assertEquals(StatusFatura.PAGA, fatura.getStatus());
        assertEquals(0, fatura.getSaldoRestante().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("500.00"), conta.getSaldoAtual());
        assertEquals(StatusLancamento.EFETIVADO, compra1.getStatus());
        assertEquals(dataComplementar, compra1.getDataEfetivacao());
        assertEquals(StatusLancamento.EFETIVADO, compra2.getStatus());
        assertEquals(dataComplementar, compra2.getDataEfetivacao());

        // 3. Estorno que volta a deixar saldo a pagar devolve as compras para PENDENTE
        PagamentoFatura pagamentoComplementar = new PagamentoFatura();
        pagamentoComplementar.setId(999L);
        pagamentoComplementar.setFatura(fatura);
        pagamentoComplementar.setConta(conta);
        pagamentoComplementar.setValor(new BigDecimal("300.00"));
        pagamentoComplementar.setDataPagamento(dataComplementar);
        when(pagamentoFaturaRepository.findByIdAndFaturaId(999L, 100L))
                .thenReturn(Optional.of(pagamentoComplementar));

        faturaService.estornarPagamento(100L, 999L);

        assertEquals(StatusFatura.FECHADA, fatura.getStatus());
        assertEquals(new BigDecimal("300.00"), fatura.getSaldoRestante());
        assertEquals(new BigDecimal("800.00"), conta.getSaldoAtual());
        assertEquals(StatusLancamento.PENDENTE, compra1.getStatus());
        assertNull(compra1.getDataEfetivacao());
        assertEquals(StatusLancamento.PENDENTE, compra2.getStatus());
        assertNull(compra2.getDataEfetivacao());
    }
}
