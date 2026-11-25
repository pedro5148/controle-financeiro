package meufinanceiro_api.faturamento.repository;

import meufinanceiro_api.faturamento.domain.CorrecaoFatura;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorrecaoFaturaRepository extends JpaRepository<CorrecaoFatura, Long> {

    boolean existsByChaveIdempotencia(String chaveIdempotencia);

    @EntityGraph(attributePaths = {"fatura"})
    List<CorrecaoFatura> findAllByFaturaIdOrderByDataHoraCorrecaoDescIdDesc(Long faturaId);
}
