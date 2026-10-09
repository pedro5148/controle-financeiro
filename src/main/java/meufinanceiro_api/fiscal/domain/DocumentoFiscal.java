package meufinanceiro_api.fiscal.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documentos_fiscais")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
public class DocumentoFiscal {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_nfce", unique = true, length = 44)
    private String chaveNfce;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "estabelecimento_id",
            foreignKey = @ForeignKey(name = "fk_documento_fiscal_estabelecimento"))
    private Estabelecimento estabelecimento;

    @OneToMany(mappedBy = "documentoFiscal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemDocumentoFiscal> itens = new ArrayList<>();

    public void adicionarItem(ItemDocumentoFiscal item) {
        itens.add(item);
        item.setDocumentoFiscal(this);
    }

    public void removerItem(ItemDocumentoFiscal item) {
        itens.remove(item);
        item.setDocumentoFiscal(null);
    }
}
