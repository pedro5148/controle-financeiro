package meufinanceiro_api.fiscal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "estabelecimentos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_estabelecimento_cnpj", columnNames = { "cnpj" })
})
@Getter
@Setter
@NoArgsConstructor
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 14)
    private String cnpj;

    @Column(nullable = false)
    private String nome;
}