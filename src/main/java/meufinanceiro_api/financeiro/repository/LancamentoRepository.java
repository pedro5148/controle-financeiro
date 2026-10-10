package meufinanceiro_api.financeiro.repository;

import jakarta.annotation.Nullable;
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

    @EntityGraph(attributePaths = { "conta", "contaDestino", "categoria", "subcategoria", "lancamentoParcelado" })
    Page<Lancamento> findAll(@Nullable Specification<Lancamento> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "conta", "contaDestino", "categoria", "subcategoria", "lancamentoParcelado" })
    Page<Lancamento> findAll(Pageable pageable);

    List<Lancamento> findByDataLancamentoBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByCategoriaId(Long categoriaId);
    boolean existsBySubcategoriaId(Long subcategoriaId);
    boolean existsByContaId(Long contaId);
    boolean existsByContaDestinoId(Long contaDestinoId);
}
