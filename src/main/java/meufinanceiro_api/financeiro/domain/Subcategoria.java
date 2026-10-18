package meufinanceiro_api.financeiro.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subcategorias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subcategoria_nome_categoria", columnNames = {"categoria_id", "nome"})
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Subcategoria {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "ativo", columnDefinition = "boolean default true", nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "categoria_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_subcategorias_categoria_id")
    )
    private Categoria categoria;

    @Version
    @Column(name = "versao", columnDefinition = "bigint default 0")
    private Long versao = 0L;
}
