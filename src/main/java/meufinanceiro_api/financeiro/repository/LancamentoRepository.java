package meufinanceiro_api.financeiro.repository;

import jakarta.annotation.Nullable;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
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
import java.util.Optional;

@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, Long>, JpaSpecificationExecutor<Lancamento> {

    @EntityGraph(attributePaths = { "conta", "contaDestino", "categoria", "subcategoria" })
    Optional<Lancamento> findById(Long id);

    @EntityGraph(attributePaths = { "conta", "contaDestino", "categoria", "subcategoria" })
    Page<Lancamento> findAll(@Nullable Specification<Lancamento> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "conta", "contaDestino", "categoria", "subcategoria" })
    Page<Lancamento> findAll(Pageable pageable);

    List<Lancamento> findByDataLancamentoBetween(LocalDate startDate, LocalDate endDate);

    List<Lancamento> findByGrupoParcelamentoIdAndStatusAndIdNot(
            Long grupoParcelamentoId,
            StatusLancamento status,
            Long id);

    Optional<Lancamento> findFirstByGrupoParcelamentoIdAndStatus(
            Long grupoParcelamentoId,
            StatusLancamento status);

    @EntityGraph(attributePaths = { "conta", "contaDestino" })
    List<Lancamento> findByGrupoParcelamentoIdAndParcelaAtualGreaterThanEqualOrderByParcelaAtualAsc(
            Long grupoParcelamentoId, Integer parcelaAtual);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsBySubcategoriaId(Long subcategoriaId);

    boolean existsByContaId(Long contaId);

    boolean existsByContaDestinoId(Long contaDestinoId);

    boolean existsByCartaoCreditoId(Long cartaoCreditoId);

    List<Lancamento> findAllByFaturaId(Long faturaId);

    @Query("""
            SELECT COALESCE(SUM(l.valor), 0)
            FROM Lancamento l
            WHERE l.status = meufinanceiro_api.financeiro.enums.StatusLancamento.EFETIVADO
              AND (
                  (l.tipo = meufinanceiro_api.financeiro.enums.TipoLancamento.RECEITA AND l.conta.id = :contaId)
                  OR (l.tipo = meufinanceiro_api.financeiro.enums.TipoLancamento.TRANSFERENCIA AND l.contaDestino.id = :contaId)
              )
            """)
    BigDecimal somarEntradasEfetivadasPorConta(@Param("contaId") Long contaId);

    @Query("""
            SELECT COALESCE(SUM(l.valor), 0)
            FROM Lancamento l
            WHERE l.status = meufinanceiro_api.financeiro.enums.StatusLancamento.EFETIVADO
              AND l.conta.id = :contaId
              AND l.tipo IN (
                  meufinanceiro_api.financeiro.enums.TipoLancamento.DESPESA,
                  meufinanceiro_api.financeiro.enums.TipoLancamento.TRANSFERENCIA
              )
            """)
    BigDecimal somarSaidasEfetivadasPorConta(@Param("contaId") Long contaId);

    @Query("""
            SELECT COALESCE(SUM(l.valor), 0)
            FROM Lancamento l
            LEFT JOIN l.conta co
            LEFT JOIN l.contaDestino cd
            WHERE l.status = meufinanceiro_api.financeiro.enums.StatusLancamento.EFETIVADO
              AND l.cartaoCreditoId IS NULL
              AND COALESCE(l.dataEfetivacao, l.dataLancamento) >= :dataCorte
              AND (
                  (l.tipo = meufinanceiro_api.financeiro.enums.TipoLancamento.RECEITA
                   AND ((:contaId IS NULL AND co.ativo = true) OR (co.id = :contaId)))
                  OR
                  (l.tipo = meufinanceiro_api.financeiro.enums.TipoLancamento.TRANSFERENCIA
                   AND ((:contaId IS NULL AND cd.ativo = true) OR (cd.id = :contaId)))
              )
            """)
    BigDecimal somarEntradasEfetivadasAPartirDe(
            @Param("dataCorte") LocalDate dataCorte,
            @Param("contaId") Long contaId);

    @Query("""
            SELECT COALESCE(SUM(l.valor), 0)
            FROM Lancamento l
            JOIN l.conta co
            WHERE l.status = meufinanceiro_api.financeiro.enums.StatusLancamento.EFETIVADO
              AND l.cartaoCreditoId IS NULL
              AND COALESCE(l.dataEfetivacao, l.dataLancamento) >= :dataCorte
              AND ((:contaId IS NULL AND co.ativo = true) OR (co.id = :contaId))
              AND l.tipo IN (
                  meufinanceiro_api.financeiro.enums.TipoLancamento.DESPESA,
                  meufinanceiro_api.financeiro.enums.TipoLancamento.TRANSFERENCIA
              )
            """)
    BigDecimal somarSaidasEfetivadasAPartirDe(
            @Param("dataCorte") LocalDate dataCorte,
            @Param("contaId") Long contaId);

    @Query("""
            SELECT COALESCE(SUM(l.valor), 0)
            FROM Lancamento l
            LEFT JOIN l.conta co
            LEFT JOIN l.contaDestino cd
            WHERE l.status = meufinanceiro_api.financeiro.enums.StatusLancamento.PENDENTE
              AND l.cartaoCreditoId IS NULL
              AND l.dataVencimento <= :dataFim
              AND (
                  (l.tipo = meufinanceiro_api.financeiro.enums.TipoLancamento.RECEITA
                   AND ((:contaId IS NULL AND co.ativo = true) OR (co.id = :contaId)))
                  OR
                  (l.tipo = meufinanceiro_api.financeiro.enums.TipoLancamento.TRANSFERENCIA
                   AND ((:contaId IS NULL AND cd.ativo = true) OR (cd.id = :contaId)))
              )
            """)
    BigDecimal somarEntradasPendentesAte(
            @Param("dataFim") LocalDate dataFim,
            @Param("contaId") Long contaId);

    @Query("""
            SELECT COALESCE(SUM(l.valor), 0)
            FROM Lancamento l
            JOIN l.conta co
            WHERE l.status = meufinanceiro_api.financeiro.enums.StatusLancamento.PENDENTE
              AND l.cartaoCreditoId IS NULL
              AND l.dataVencimento <= :dataFim
              AND ((:contaId IS NULL AND co.ativo = true) OR (co.id = :contaId))
              AND l.tipo IN (
                  meufinanceiro_api.financeiro.enums.TipoLancamento.DESPESA,
                  meufinanceiro_api.financeiro.enums.TipoLancamento.TRANSFERENCIA
              )
            """)
    BigDecimal somarSaidasPendentesAte(
            @Param("dataFim") LocalDate dataFim,
            @Param("contaId") Long contaId);

    public interface ResumoPendenteTipoProjection {
        TipoLancamento getTipo();
        Long getQuantidade();
        BigDecimal getValorTotal();
    }

    @Query("""
            SELECT l.tipo AS tipo,
                   COUNT(l) AS quantidade,
                   COALESCE(SUM(l.valor), 0) AS valorTotal
            FROM Lancamento l
            WHERE l.status = meufinanceiro_api.financeiro.enums.StatusLancamento.PENDENTE
              AND l.cartaoCreditoId IS NULL
              AND l.dataVencimento BETWEEN :dataInicio AND :dataFim
              AND (:contaId IS NULL OR l.conta.id = :contaId)
            GROUP BY l.tipo
            """)
    List<ResumoPendenteTipoProjection> resumirPendentesNormaisPorPeriodo(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("contaId") Long contaId);

    public interface LancamentoSugestaoProjection {
        String getDescricao();
        Long getCategoriaId();
        String getCategoriaNome();
        Boolean getCategoriaAtivo();
        Long getSubcategoriaId();
        String getSubcategoriaNome();
        Boolean getSubcategoriaAtivo();
    }

    @Query(value = """
            WITH lancamentos_filtrados AS (
                SELECT
                    TRIM(l.descricao) AS descricao,
                    LOWER(TRIM(l.descricao)) AS descricao_key,
                    l.id,
                    l.data_lancamento,
                    c.id AS categoria_id,
                    c.nome AS categoria_nome,
                    c.ativo AS categoria_ativo,
                    s.id AS subcategoria_id,
                    s.nome AS subcategoria_nome,
                    s.ativo AS subcategoria_ativo,
                    word_similarity(:termo, l.descricao) AS sim_score
                FROM lancamentos l
                JOIN categorias c ON c.id = l.categoria_id
                LEFT JOIN subcategorias s ON s.id = l.subcategoria_id
                WHERE (:tipo IS NULL OR l.tipo = :tipo)
                  AND (
                      l.descricao ILIKE CONCAT('%', :termo, '%')
                      OR (
                          CHAR_LENGTH(:termo) >= 6
                          AND word_similarity(:termo, l.descricao) >= 0.6
                      )
                  )
            ),
            agrupado AS (
                SELECT DISTINCT ON (descricao_key)
                    descricao,
                    categoria_id AS categoriaId,
                    categoria_nome AS categoriaNome,
                    categoria_ativo AS categoriaAtivo,
                    subcategoria_id AS subcategoriaId,
                    subcategoria_nome AS subcategoriaNome,
                    subcategoria_ativo AS subcategoriaAtivo,
                    sim_score,
                    COUNT(*) OVER (PARTITION BY descricao_key) AS frequencia,
                    id AS ultimo_id
                FROM lancamentos_filtrados
                ORDER BY
                    descricao_key,
                    categoria_ativo DESC,
                    (subcategoria_id IS NULL OR subcategoria_ativo = true) DESC,
                    data_lancamento DESC,
                    id DESC
            )
            SELECT
                descricao,
                categoriaId,
                categoriaNome,
                categoriaAtivo,
                subcategoriaId,
                subcategoriaNome,
                subcategoriaAtivo
            FROM agrupado
            ORDER BY
                CASE WHEN LOWER(descricao) LIKE LOWER(CONCAT(:termo, '%')) THEN 0 ELSE 1 END,
                sim_score DESC,
                frequencia DESC,
                ultimo_id DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<LancamentoSugestaoProjection> buscarSugestoesPorDescricao(
            @Param("termo") String termo,
            @Param("tipo") String tipo,
            @Param("limite") int limite);
}
