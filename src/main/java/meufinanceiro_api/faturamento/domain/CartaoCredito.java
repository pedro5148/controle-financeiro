package meufinanceiro_api.faturamento.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "cartoes_credito")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class CartaoCredito {

    private static final DateTimeFormatter MES_ANO_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nome;

    @Column(name = "limite_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal limiteTotal;

    @Column(name = "dia_fechamento", nullable = false)
    private Integer diaFechamento;

    @Column(name = "dia_vencimento", nullable = false)
    private Integer diaVencimento;

    @Column(name = "conta_padrao_id", nullable = false)
    private Long contaPadraoId;

    @Column(length = 50)
    private String bandeira;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean principal = false;

    @Column(name = "ativo", columnDefinition = "boolean default true", nullable = false)
    private boolean ativo = true;

    @Version
    @Column(name = "versao", columnDefinition = "bigint default 0")
    private Long versao = 0L;

    /**
     * Calcula a competência (YearMonth de vencimento) da fatura base para uma compra realizada na data informada.
     */
    public YearMonth calcularCompetenciaFatura(LocalDate dataCompra) {
        YearMonth ymCompra = YearMonth.from(dataCompra);
        LocalDate dataFechamentoNoMes = ymCompra.atDay(Math.min(this.diaFechamento, ymCompra.lengthOfMonth()));

        YearMonth ymFechamento = dataCompra.isBefore(dataFechamentoNoMes)
                ? ymCompra
                : ymCompra.plusMonths(1);

        return (this.diaVencimento <= this.diaFechamento)
                ? ymFechamento.plusMonths(1)
                : ymFechamento;
    }

    public LocalDate calcularDataVencimento(YearMonth competenciaVencimento) {
        return competenciaVencimento.atDay(Math.min(this.diaVencimento, competenciaVencimento.lengthOfMonth()));
    }

    public LocalDate calcularDataFechamento(YearMonth competenciaVencimento) {
        YearMonth ymFechamento = (this.diaVencimento <= this.diaFechamento)
                ? competenciaVencimento.minusMonths(1)
                : competenciaVencimento;
        return ymFechamento.atDay(Math.min(this.diaFechamento, ymFechamento.lengthOfMonth()));
    }

    public static String formatarMesAno(YearMonth competencia) {
        return competencia.format(MES_ANO_FORMATTER);
    }

    public static YearMonth parseMesAno(String mesAno) {
        return YearMonth.parse(mesAno, MES_ANO_FORMATTER);
    }
}