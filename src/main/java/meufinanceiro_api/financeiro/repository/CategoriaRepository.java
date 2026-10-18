package meufinanceiro_api.financeiro.repository;

import meufinanceiro_api.financeiro.domain.Categoria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    @EntityGraph(attributePaths = "subcategorias")
    List<Categoria> findAllByOrderByNomeAsc();

    @EntityGraph(attributePaths = "subcategorias")
    List<Categoria> findAllByAtivoTrueOrderByNomeAsc();

    @EntityGraph(attributePaths = "subcategorias")
    Optional<Categoria> findWithSubcategoriasById(Long id);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
