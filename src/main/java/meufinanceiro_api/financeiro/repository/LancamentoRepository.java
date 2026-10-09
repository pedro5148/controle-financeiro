package meufinanceiro_api.financeiro.repository;

import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, Long>, JpaSpecificationExecutor<Lancamento> {

    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l " +
            "WHERE l.tipo = :tipo AND l.status = 'EFETIVADO' " +
            "AND l.dataEfetivacao < :dataLimite")
    BigDecimal somarEfetivadosAteData(@Param("tipo") TipoLancamento tipo,
                                      @Param("dataLimite") LocalDate dataLimite);

    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l " +
            "WHERE l.tipo = :tipo AND l.status = 'EFETIVADO' " +
            "AND l.meioPagamento != 'CREDITO' " +
            "AND l.dataEfetivacao < :dataLimite")
    BigDecimal somarEfetivadosAteDataIgnorandoCredito(@Param("tipo") TipoLancamento tipo,
                                                      @Param("dataLimite") LocalDate dataLimite);

    // 2. Uso correto de Soft Links
    @Query("SELECT l.cartaoCreditoId, SUM(l.valor) FROM Lancamento l " +
            "WHERE l.faturaId IS NOT NULL AND l.tipo = :tipoDespesa " +
            "AND (:mes IS NULL OR EXTRACT(MONTH FROM l.dataLancamento) = :mes) " +
            "AND (:ano IS NULL OR EXTRACT(YEAR FROM l.dataLancamento) = :ano) " +
            "GROUP BY l.cartaoCreditoId")
    List<Object[]> findValorFaturaAgrupadoPorCartao(@Param("mes") Integer mes,
                                                    @Param("ano") Integer ano,
                                                    @Param("tipoDespesa") TipoLancamento tipoDespesa);

    List<Lancamento> findByFaturaId(Long faturaId);

    long countByFaturaId(Long faturaId);

    // 3. EntityGraphs saneados (prevenção do N+1 sem quebrar por vínculos inexistentes)
    @EntityGraph(attributePaths = { "conta", "contaDestino", "categoria", "subcategoria", "lancamentoParcelado" })
    Page<Lancamento> findAll(Specification<Lancamento> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "conta", "contaDestino", "categoria", "subcategoria", "lancamentoParcelado" })
    Page<Lancamento> findAll(Pageable pageable);

    List<Lancamento> findByDataLancamentoBetween(LocalDate startDate, LocalDate endDate);

    boolean existsBySubcategoriaCategoriaId(Long categoriaId);

    boolean existsBySubcategoriaId(Long subcategoriaId);

    boolean existsByContaId(Long contaId);

    boolean existsByContaDestinoId(Long contaDestinoId);
}
