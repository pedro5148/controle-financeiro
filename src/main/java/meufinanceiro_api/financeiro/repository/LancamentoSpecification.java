package meufinanceiro_api.financeiro.repository;

import jakarta.persistence.criteria.Predicate;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.enums.MeioPagamento;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class LancamentoSpecification {
    public static Specification<Lancamento> filtroAvancado(
            String descricao,
            TipoLancamento tipo,
            StatusLancamento status,
            Long contaId,
            Long categoriaId,
            Long subcategoriaId,
            Long grupoParcelamentoId,
            Long cartaoCreditoId,
            Long faturaId,
            Integer mes,
            Integer ano) {

        if (mes != null && (mes < 1 || mes > 12)) {
            throw new RegraNegocioException("O mês informado deve estar entre 1 e 12.");
        }

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (descricao != null && !descricao.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("descricao")), "%" + descricao.trim().toLowerCase() + "%"));
            }

            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipo"), tipo));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (contaId != null) {
                predicates.add(cb.equal(root.get("conta").get("id"), contaId));
            }

            if (categoriaId != null) {
                predicates.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }

            if (subcategoriaId != null) {
                predicates.add(cb.equal(root.get("subcategoria").get("id"), subcategoriaId));
            }

            if (grupoParcelamentoId != null) {
                predicates.add(cb.equal(root.get("grupoParcelamentoId"), grupoParcelamentoId));
            }

            if (cartaoCreditoId != null) {
                predicates.add(cb.equal(root.get("cartaoCreditoId"), cartaoCreditoId));
            }

            if (faturaId != null) {
                predicates.add(cb.equal(root.get("faturaId"), faturaId));
            }

            if (mes != null || ano != null) {
                LocalDate start;
                LocalDate end;
                if (mes != null) {
                    int anoFiltro = (ano != null) ? ano : LocalDate.now().getYear();
                    YearMonth ym = YearMonth.of(anoFiltro, mes);
                    start = ym.atDay(1);
                    end = ym.atEndOfMonth();
                } else {
                    start = LocalDate.of(ano, 1, 1);
                    end = LocalDate.of(ano, 12, 31);
                }

                Predicate filtroCreditoPorVencimento = cb.and(
                        cb.equal(root.get("meioPagamento"), MeioPagamento.CREDITO),
                        cb.between(root.get("dataVencimento"), start, end)
                );
                Predicate filtroNormalPorLancamento = cb.and(
                        cb.notEqual(root.get("meioPagamento"), MeioPagamento.CREDITO),
                        cb.between(root.get("dataLancamento"), start, end)
                );
                predicates.add(cb.or(filtroCreditoPorVencimento, filtroNormalPorLancamento));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}