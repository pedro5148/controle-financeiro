package meufinanceiro_api.faturamento.domain;

import jakarta.persistence.*;
import lombok.*;
import meufinanceiro_api.financeiro.domain.Conta;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pagamentos_faturas")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoFatura {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "fatura_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pagamentos_faturas_fatura_id")
    )
    private Fatura fatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "conta_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pagamentos_faturas_conta_id")
    )
    private Conta conta;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @Column(columnDefinition = "TEXT")
    private String observacoes;
}

