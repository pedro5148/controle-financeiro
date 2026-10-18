package meufinanceiro_api.financeiro.repository;

import meufinanceiro_api.financeiro.domain.Subcategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Long> {
    List<Subcategoria> findAllByOrderByNomeAsc();
    List<Subcategoria> findAllByAtivoTrueOrderByNomeAsc();
    List<Subcategoria> findAllByCategoriaIdOrderByNomeAsc(Long categoriaId);
    List<Subcategoria> findAllByCategoriaIdAndAtivoTrueOrderByNomeAsc(Long categoriaId);
    boolean existsByCategoriaIdAndNomeIgnoreCase(Long categoriaId, String nome);
    boolean existsByCategoriaIdAndNomeIgnoreCaseAndIdNot(Long categoriaId, String nome, Long id);
}
