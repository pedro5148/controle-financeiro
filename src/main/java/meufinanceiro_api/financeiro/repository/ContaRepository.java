package meufinanceiro_api.financeiro.repository;

import jakarta.persistence.LockModeType;
import meufinanceiro_api.financeiro.domain.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conta c WHERE c.id = :id")
    Optional<Conta> findByIdForUpdate(@Param("id") Long id);

    List<Conta> findAllByOrderByPadraoDescNomeAsc();
    List<Conta> findAllByAtivoTrueOrderByPadraoDescNomeAsc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Conta c SET c.padrao = false WHERE c.padrao = true AND (:idIgnorar IS NULL OR c.id != :idIgnorar)")
    void removerFlagPadraoDeOutrasContas(@Param("idIgnorar") Long idIgnorar);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    @Query("SELECT COALESCE(SUM(c.saldoAtual), 0) FROM Conta c WHERE c.ativo = true")
    BigDecimal somarSaldoTotalContasAtivas();

    long countByAtivoTrue();
}