package meufinanceiro_api.financeiro.repository;

import jakarta.annotation.Nullable;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsBySubcategoriaId(Long subcategoriaId);

    boolean existsByContaId(Long contaId);

    boolean existsByContaDestinoId(Long contaDestinoId);
}
