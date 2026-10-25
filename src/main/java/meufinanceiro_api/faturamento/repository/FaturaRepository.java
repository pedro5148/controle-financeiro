package meufinanceiro_api.faturamento.repository;

import meufinanceiro_api.faturamento.domain.Fatura;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FaturaRepository extends JpaRepository<Fatura, Long> {

    @EntityGraph(attributePaths = {"cartao"})
    Optional<Fatura> findById(Long id);

    @EntityGraph(attributePaths = {"cartao"})
    Optional<Fatura> findByCartaoIdAndMesAno(Long cartaoId, String mesAno);

    @EntityGraph(attributePaths = {"cartao"})
    @Query("""
            SELECT f FROM Fatura f
            WHERE (:cartaoCreditoId IS NULL OR f.cartao.id = :cartaoCreditoId)
              AND (:mesAno IS NULL OR f.mesAno = :mesAno)
              AND (:status IS NULL OR f.status = :status)
            ORDER BY f.dataVencimento DESC, f.id DESC
            """)
    List<Fatura> listarComFiltros(
            @Param("cartaoCreditoId") Long cartaoCreditoId,
            @Param("mesAno") String mesAno,
            @Param("status") StatusFatura status
    );

    @Query("""
            SELECT COALESCE(SUM(f.valorTotal - f.valorPago), 0)
            FROM Fatura f
            WHERE f.cartao.id = :cartaoId
              AND f.status <> meufinanceiro_api.faturamento.enums.StatusFatura.PAGA
            """)
    BigDecimal somarLimiteUtilizadoPorCartao(@Param("cartaoId") Long cartaoId);

    boolean existsByCartaoId(Long cartaoId);

    interface ResumoFaturaPendenteProjection {
        Long getQuantidade();
        BigDecimal getValorTotal();
    }

    @Query("""
            SELECT COUNT(f) AS quantidade,
                   COALESCE(SUM(f.valorTotal - f.valorPago), 0) AS valorTotal
            FROM Fatura f
            WHERE f.mesAno = :mesAno
              AND f.status <> meufinanceiro_api.faturamento.enums.StatusFatura.PAGA
              AND (f.valorTotal - f.valorPago) > 0
              AND (:contaId IS NULL OR f.cartao.contaPadraoId = :contaId)
            """)
    ResumoFaturaPendenteProjection resumirFaturasPendentesPorMesAno(
            @Param("mesAno") String mesAno,
            @Param("contaId") Long contaId);

    @Query("""
            SELECT COALESCE(SUM(f.valorTotal - f.valorPago), 0)
            FROM Fatura f
            JOIN meufinanceiro_api.financeiro.domain.Conta co ON co.id = f.cartao.contaPadraoId
            WHERE f.status <> meufinanceiro_api.faturamento.enums.StatusFatura.PAGA
              AND (f.valorTotal - f.valorPago) > 0
              AND f.dataVencimento <= :dataFim
              AND ((:contaId IS NULL AND co.ativo = true) OR (co.id = :contaId))
            """)
    BigDecimal somarFaturasPendentesAte(
            @Param("dataFim") LocalDate dataFim,
            @Param("contaId") Long contaId);
}

