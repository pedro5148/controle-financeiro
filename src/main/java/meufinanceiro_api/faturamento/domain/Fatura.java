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
        BigDecimal resultado = base.subtract(valor);
        this.valorTotal = resultado.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : resultado;
    }

    public BigDecimal getSaldoRestante() {
        BigDecimal total = (this.valorTotal != null) ? this.valorTotal : BigDecimal.ZERO;
        BigDecimal pago = (this.valorPago != null) ? this.valorPago : BigDecimal.ZERO;
        BigDecimal restante = total.subtract(pago);
        return restante.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : restante;
    }
}