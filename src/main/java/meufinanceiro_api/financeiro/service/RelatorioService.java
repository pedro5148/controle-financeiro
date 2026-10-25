package meufinanceiro_api.financeiro.service;

import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.dto.CategoriaSubcategoriaResumoProjection;
import meufinanceiro_api.financeiro.dto.RelatorioCategoriaItemDTO;
import meufinanceiro_api.financeiro.dto.RelatorioPorCategoriaResponseDTO;
import meufinanceiro_api.financeiro.dto.RelatorioSubcategoriaItemDTO;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final LancamentoRepository lancamentoRepository;

    @Transactional(readOnly = true)
    public RelatorioPorCategoriaResponseDTO gerarRelatorioPorCategoria(
            Integer mes,
            Integer ano,
            TipoLancamento tipo,
            StatusLancamento status,
            Long contaId,
            Long cartaoCreditoId) {

        LocalDate hoje = LocalDate.now();
        int mesFiltro = (mes != null) ? mes : hoje.getMonthValue();
        int anoFiltro = (ano != null) ? ano : hoje.getYear();
        TipoLancamento tipoFiltro = (tipo != null) ? tipo : TipoLancamento.DESPESA;

        if (mesFiltro < 1 || mesFiltro > 12) {
            throw new RegraNegocioException("O mês informado deve estar entre 1 e 12.");
        }

        if (contaId != null && cartaoCreditoId != null) {
            throw new RegraNegocioException("Não é possível filtrar simultaneamente por conta bancária e cartão de crédito.");
        }

        YearMonth competencia = YearMonth.of(anoFiltro, mesFiltro);
        LocalDate dataInicio = competencia.atDay(1);
        LocalDate dataFim = competencia.atEndOfMonth();

        List<CategoriaSubcategoriaResumoProjection> linhas = lancamentoRepository.agruparPorCategoriaESubcategoria(
                tipoFiltro,
                status,
                contaId,
                cartaoCreditoId,
                dataInicio,
                dataFim
        );

        BigDecimal valorTotalPeriodo = BigDecimal.ZERO;
        long quantidadeTotalLancamentos = 0L;
        Map<Long, List<CategoriaSubcategoriaResumoProjection>> porCategoria = new LinkedHashMap<>();

        for (CategoriaSubcategoriaResumoProjection linha : linhas) {
            BigDecimal valorLinha = linha.valorTotal() != null ? linha.valorTotal() : BigDecimal.ZERO;
            long qtdLinha = linha.quantidadeLancamentos() != null ? linha.quantidadeLancamentos() : 0L;

            valorTotalPeriodo = valorTotalPeriodo.add(valorLinha);
            quantidadeTotalLancamentos += qtdLinha;

            porCategoria.computeIfAbsent(linha.categoriaId(), k -> new ArrayList<>()).add(linha);
        }

        final BigDecimal totalGeralFinal = valorTotalPeriodo;
        List<RelatorioCategoriaItemDTO> categorias = new ArrayList<>();

        for (Map.Entry<Long, List<CategoriaSubcategoriaResumoProjection>> entry : porCategoria.entrySet()) {
            List<CategoriaSubcategoriaResumoProjection> itensCategoria = entry.getValue();
            String categoriaNome = itensCategoria.get(0).categoriaNome();

            BigDecimal valorTotalCategoria = BigDecimal.ZERO;
            long quantidadeCategoria = 0L;
            BigDecimal valorSemSubcategoria = BigDecimal.ZERO;

            for (CategoriaSubcategoriaResumoProjection item : itensCategoria) {
                BigDecimal valorItem = item.valorTotal() != null ? item.valorTotal() : BigDecimal.ZERO;
                long qtdItem = item.quantidadeLancamentos() != null ? item.quantidadeLancamentos() : 0L;

                valorTotalCategoria = valorTotalCategoria.add(valorItem);
                quantidadeCategoria += qtdItem;

                if (item.subcategoriaId() == null) {
                    valorSemSubcategoria = valorSemSubcategoria.add(valorItem);
                }
            }

            final BigDecimal totalCatFinal = valorTotalCategoria;
            List<RelatorioSubcategoriaItemDTO> subcategorias = itensCategoria.stream()
                    .filter(item -> item.subcategoriaId() != null)
                    .map(item -> {
                        BigDecimal valorSub = item.valorTotal() != null ? item.valorTotal() : BigDecimal.ZERO;
                        long qtdSub = item.quantidadeLancamentos() != null ? item.quantidadeLancamentos() : 0L;
                        return new RelatorioSubcategoriaItemDTO(
                                item.subcategoriaId(),
                                item.subcategoriaNome(),
                                valorSub,
                                qtdSub,
                                calcularPercentual(valorSub, totalCatFinal),
                                calcularPercentual(valorSub, totalGeralFinal)
                        );
                    })
                    .sorted(Comparator.comparing(RelatorioSubcategoriaItemDTO::valorTotal).reversed()
                            .thenComparing(RelatorioSubcategoriaItemDTO::subcategoriaNome, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            categorias.add(new RelatorioCategoriaItemDTO(
                    entry.getKey(),
                    categoriaNome,
                    valorTotalCategoria,
                    quantidadeCategoria,
                    calcularPercentual(valorTotalCategoria, totalGeralFinal),
                    valorSemSubcategoria,
                    subcategorias
            ));
        }

        categorias.sort(Comparator.comparing(RelatorioCategoriaItemDTO::valorTotal).reversed()
                .thenComparing(RelatorioCategoriaItemDTO::categoriaNome, String.CASE_INSENSITIVE_ORDER));

        return new RelatorioPorCategoriaResponseDTO(
                mesFiltro,
                anoFiltro,
                tipoFiltro,
                valorTotalPeriodo,
                quantidadeTotalLancamentos,
                categorias
        );
    }

    private BigDecimal calcularPercentual(BigDecimal parte, BigDecimal total) {
        if (parte == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return parte.multiply(CEM).divide(total, 2, RoundingMode.HALF_UP);
    }
}

