package meufinanceiro_api.fiscal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import meufinanceiro_api.financeiro.domain.Lancamento;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_documentos_fiscais")
@Getter
@Setter
@NoArgsConstructor
public class ItemDocumentoFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "documento_fiscal_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_documento_fiscal"))
    private DocumentoFiscal documentoFiscal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "produto_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_produto"))
    private Produto produto;

    @Column(name = "descricao_cupom", length = 100)
    private String descricaoCupom;

    @Column(name = "codigo_cupom", length = 50)
    private String codigoCupom;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantidade;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal valorUnitarioBruto;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotalBruto;
}
