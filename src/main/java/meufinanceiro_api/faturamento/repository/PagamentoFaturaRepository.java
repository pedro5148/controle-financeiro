package meufinanceiro_api.faturamento.repository;

import meufinanceiro_api.faturamento.domain.PagamentoFatura;
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
public interface PagamentoFaturaRepository extends JpaRepository<PagamentoFatura, Long> {

    @EntityGraph(attributePaths = {"fatura", "conta"})
    List<PagamentoFatura> findAllByFaturaIdOrderByDataPagamentoDescIdDesc(Long faturaId);

    @EntityGraph(attributePaths = {"fatura", "conta"})
    Optional<PagamentoFatura> findByIdAndFaturaId(Long id, Long faturaId);

    Optional<PagamentoFatura> findFirstByFaturaIdOrderByDataPagamentoDescIdDesc(Long faturaId);

    boolean existsByContaId(Long contaId);

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM PagamentoFatura p WHERE p.conta.id = :contaId")
    BigDecimal somarTotalPagoPorConta(@Param("contaId") Long contaId);

    @Query("""
            SELECT COALESCE(SUM(p.valor), 0)
            FROM PagamentoFatura p
            JOIN p.conta co
            WHERE p.dataPagamento >= :dataCorte
              AND ((:contaId IS NULL AND co.ativo = true) OR (co.id = :contaId))
            """)
    BigDecimal somarPagamentosAPartirDe(
            @Param("dataCorte") LocalDate dataCorte,
            @Param("contaId") Long contaId);
}
