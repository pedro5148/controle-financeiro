package meufinanceiro_api.financeiro.domain;

import jakarta.persistence.*;
import lombok.*;
import meufinanceiro_api.financeiro.enums.MeioPagamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.enums.TipoRecorrencia;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lancamentos", uniqueConstraints = { })
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Lancamento {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLancamento tipo;

    @Column(nullable = false, length = 100)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "conta_id",
            //nullable = false,
            foreignKey = @ForeignKey(name = "fk_lancamentos_conta_id"))
    private Conta conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "conta_destino_id",
            foreignKey = @ForeignKey(name = "fk_lancamentos_conta_destino_id"))
    private Conta contaDestino; // Apenas para transferências

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "categoria_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_lancamentos_categoria_id"))
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subcategoria_id",
            foreignKey = @ForeignKey(name = "fk_lancamentos_subcategoria_id"))
    private Subcategoria subcategoria;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDate dataLancamento;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_efetivacao")
    private LocalDate dataEfetivacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusLancamento status;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recorrencia", nullable = false)
    private TipoRecorrencia tipoRecorrencia = TipoRecorrencia.NENHUMA;

    @Column(name = "parcela_atual")
    private Integer parcelaAtual;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;

    @Column(name = "grupo_parcelamento_id")
    private Long grupoParcelamentoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "meio_pagamento", nullable = false)
    private MeioPagamento meioPagamento;

    @Column(name = "cartao_credito_id")
    private Long cartaoCreditoId;

    @Column(name = "documento_fiscal_id")
    private Long documentoFiscalId;

    @Column(name = "fatura_id")
    private Long faturaId;

}
