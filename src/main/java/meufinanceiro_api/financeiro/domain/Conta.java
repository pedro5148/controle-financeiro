package meufinanceiro_api.financeiro.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contas")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Conta {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nome;

    @Column(name = "saldo_inicial", precision = 19, scale = 2, nullable = false)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "saldo_atual", precision = 19, scale = 2, nullable = false)
    private BigDecimal saldoAtual = BigDecimal.ZERO;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDate dataCriacao;

    @Column(name = "padrao", columnDefinition = "boolean default false")
    private boolean padrao = false;

    @Column(name = "ativo", columnDefinition = "boolean default true", nullable = false)
    private boolean ativo = true;

    @Version
    private Long versao;

    @PrePersist
    protected void onCreate() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDate.now();
        }
    }
}
