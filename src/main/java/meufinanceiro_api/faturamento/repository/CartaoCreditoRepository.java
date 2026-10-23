package meufinanceiro_api.faturamento.repository;

import meufinanceiro_api.faturamento.domain.CartaoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, Long> {

    List<CartaoCredito> findAllByOrderByPrincipalDescNomeAsc();

    List<CartaoCredito> findAllByAtivoTrueOrderByPrincipalDescNomeAsc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CartaoCredito c SET c.principal = false WHERE c.principal = true AND (:idIgnorar IS NULL OR c.id != :idIgnorar)")
    void removerFlagPrincipalDeOutrosCartoes(@Param("idIgnorar") Long idIgnorar);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    boolean existsByContaPadraoId(Long contaPadraoId);
}

