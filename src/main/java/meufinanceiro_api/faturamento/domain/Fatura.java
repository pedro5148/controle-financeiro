package meufinanceiro_api.faturamento.domain;

import jakarta.persistence.*;
import lombok.*;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "faturas",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_faturas_cartao_mes_ano", columnNames = {"cartao_credito_id", "mes_ano"})
        }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Fatura {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cartao_credito_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_faturas_cartao_credito_id")
    )
    private CartaoCredito cartao;

    @Column(name = "mes_ano", nullable = false, length = 7)
    private String mesAno; // "MM/yyyy"

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "valor_pago", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusFatura status = StatusFatura.ABERTA;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_fechamento", nullable = false)
    private LocalDate dataFechamento;

    @Version
    @Column(name = "versao", columnDefinition = "bigint default 0")
    private Long versao = 0L;

    public boolean isAberta() {
        return this.status == StatusFatura.ABERTA;
    }

    public void validarAbertaParaMovimentacao() {
        if (!isAberta()) {
            throw new RegraNegocioException(
                    "Não é permitido incluir, alterar ou excluir lançamentos na fatura " + this.mesAno +
                    " pois ela se encontra com status " + this.status + "."
            );
        }
    }

    public void adicionarValor(BigDecimal valor) {
        validarAbertaParaMovimentacao();
        BigDecimal base = (this.valorTotal != null) ? this.valorTotal : BigDecimal.ZERO;
        this.valorTotal = base.add(valor);
    }

    public void removerValor(BigDecimal valor) {
        validarAbertaParaMovimentacao();
        BigDecimal base = (this.valorTotal != null) ? this.valorTotal : BigDecimal.ZERO;
        BigDecimal pago = (this.valorPago != null) ? this.valorPago : BigDecimal.ZERO;
        BigDecimal resultado = base.subtract(valor);
        if (resultado.compareTo(BigDecimal.ZERO) < 0) {
            resultado = BigDecimal.ZERO;
        }
        if (resultado.compareTo(pago) < 0) {
            throw new RegraNegocioException(
                    String.format("Não é possível remover ou reduzir este lançamento pois o valor total da fatura (R$ %s) ficaria menor que o valor já pago/adiantado (R$ %s). Estorne o pagamento antes de realizar esta operação.",
                            resultado, pago)
            );
        }
        this.valorTotal = resultado;
    }

    public void fechar() {
        if (this.status != StatusFatura.ABERTA) {
            throw new RegraNegocioException(
                    "Apenas faturas com status ABERTA podem ser fechadas. Status atual: " + this.status + "."
            );
        }
        BigDecimal total = (this.valorTotal != null) ? this.valorTotal : BigDecimal.ZERO;
        if (total.compareTo(BigDecimal.ZERO) > 0 && getSaldoRestante().compareTo(BigDecimal.ZERO) == 0) {
            this.status = StatusFatura.PAGA;
        } else {
            this.status = StatusFatura.FECHADA;
        }
    }

    public void reabrir() {
        if (this.status == StatusFatura.ABERTA) {
            throw new RegraNegocioException("A fatura " + this.mesAno + " já se encontra ABERTA.");
        }
        if (this.status == StatusFatura.PAGA) {
            throw new RegraNegocioException(
                    "Não é possível reabrir uma fatura com status PAGA. Estorne o pagamento da fatura antes de reabri-la."
            );
        }
        this.status = StatusFatura.ABERTA;
    }

    public void registrarPagamento(BigDecimal valor) {
        if (this.status == StatusFatura.PAGA) {
            throw new RegraNegocioException("A fatura " + this.mesAno + " já se encontra totalmente paga.");
        }
        BigDecimal restante = getSaldoRestante();
        if (restante.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("Não há saldo restante a pagar na fatura " + this.mesAno + ".");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor do pagamento deve ser maior que zero.");
        }
        if (valor.compareTo(restante) > 0) {
            throw new RegraNegocioException(
                    String.format("O valor do pagamento (R$ %s) não pode ser maior que o saldo restante da fatura (R$ %s).",
                            valor, restante)
            );
        }

        BigDecimal pagoAtual = (this.valorPago != null) ? this.valorPago : BigDecimal.ZERO;
        this.valorPago = pagoAtual.add(valor);

        // Se a fatura não estiver ABERTA e o saldo restante zerou, passa automaticamente para PAGA
        if (!isAberta() && getSaldoRestante().compareTo(BigDecimal.ZERO) == 0) {
            this.status = StatusFatura.PAGA;
        }
    }

    public void estornarPagamento(BigDecimal valorEstornado) {
        BigDecimal pagoAtual = (this.valorPago != null) ? this.valorPago : BigDecimal.ZERO;
        BigDecimal novoValorPago = pagoAtual.subtract(valorEstornado);
        this.valorPago = novoValorPago.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : novoValorPago;

        if (this.status == StatusFatura.PAGA && getSaldoRestante().compareTo(BigDecimal.ZERO) > 0) {
            this.status = StatusFatura.FECHADA;
        }
    }

    public BigDecimal getSaldoRestante() {
        BigDecimal total = (this.valorTotal != null) ? this.valorTotal : BigDecimal.ZERO;
        BigDecimal pago = (this.valorPago != null) ? this.valorPago : BigDecimal.ZERO;
        BigDecimal restante = total.subtract(pago);
        return restante.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : restante;
    }
}