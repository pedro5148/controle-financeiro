package meufinanceiro_api.faturamento.domain;

import jakarta.persistence.*;
import lombok.*;
import meufinanceiro_api.faturamento.enums.StatusFatura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "correcoes_faturas",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_correcoes_faturas_chave_idempotencia",
                        columnNames = {"chave_idempotencia"}
                )
        }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class CorrecaoFatura {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "fatura_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_correcoes_faturas_fatura_id")
    )
    private Fatura fatura;

    @Column(name = "chave_idempotencia", nullable = false, length = 100)
    private String chaveIdempotencia;

    @Column(name = "lancamento_id", nullable = false)
    private Long lancamentoId;

    @Column(name = "descricao_despesa", nullable = false, length = 100)
    private String descricaoDespesa;

    @Column(name = "valor_despesa", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorDespesa;

    @Column(name = "data_compra", nullable = false)
    private LocalDate dataCompra;

    @Column(name = "pagamento_corrigido", nullable = false)
    private boolean pagamentoCorrigido;

    @Column(name = "pagamento_fatura_id")
    private Long pagamentoFaturaId;

    @Column(name = "conta_anterior_id")
    private Long contaAnteriorId;

    @Column(name = "conta_nova_id")
    private Long contaNovaId;

    @Column(name = "data_pagamento_anterior")
    private LocalDate dataPagamentoAnterior;

    @Column(name = "data_pagamento_nova")
    private LocalDate dataPagamentoNova;

    @Column(name = "valor_pagamento_anterior", precision = 19, scale = 2)
    private BigDecimal valorPagamentoAnterior;

    @Column(name = "valor_pagamento_novo", precision = 19, scale = 2)
    private BigDecimal valorPagamentoNovo;

    @Column(name = "valor_total_anterior", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotalAnterior;

    @Column(name = "valor_total_novo", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotalNovo;

    @Column(name = "valor_pago_anterior", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorPagoAnterior;

    @Column(name = "valor_pago_novo", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorPagoNovo;

    @Column(name = "saldo_restante_anterior", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoRestanteAnterior;

    @Column(name = "saldo_restante_novo", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoRestanteNovo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", nullable = false)
    private StatusFatura statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false)
    private StatusFatura statusNovo;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "data_hora_correcao", nullable = false)
    private LocalDateTime dataHoraCorrecao;
}
