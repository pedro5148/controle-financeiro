package meufinanceiro_api.faturamento.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.faturamento.domain.CartaoCredito;
import meufinanceiro_api.faturamento.domain.CorrecaoFatura;
import meufinanceiro_api.faturamento.domain.Fatura;
import meufinanceiro_api.faturamento.domain.PagamentoFatura;
import meufinanceiro_api.faturamento.dto.CorrecaoFaturaHistoricoResponseDTO;
import meufinanceiro_api.faturamento.dto.CorrecaoFaturaResponseDTO;
import meufinanceiro_api.faturamento.dto.CorrigirFaturaPagaRequestDTO;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import meufinanceiro_api.faturamento.mapper.CorrecaoFaturaMapper;
import meufinanceiro_api.faturamento.mapper.FaturaMapper;
import meufinanceiro_api.faturamento.mapper.PagamentoFaturaMapper;
import meufinanceiro_api.faturamento.repository.CorrecaoFaturaRepository;
import meufinanceiro_api.faturamento.repository.FaturaRepository;
import meufinanceiro_api.faturamento.repository.PagamentoFaturaRepository;
import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.domain.Conta;
import meufinanceiro_api.financeiro.domain.Lancamento;
import meufinanceiro_api.financeiro.domain.Subcategoria;
import meufinanceiro_api.financeiro.enums.StatusLancamento;
import meufinanceiro_api.financeiro.enums.TipoLancamento;
import meufinanceiro_api.financeiro.enums.TipoRecorrencia;
import meufinanceiro_api.financeiro.exception.ConflitoNegocioException;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.LancamentoMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.ContaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.SubcategoriaRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CorrecaoFaturaService {

    private final FaturaRepository faturaRepository;
    private final PagamentoFaturaRepository pagamentoFaturaRepository;
    private final CorrecaoFaturaRepository correcaoFaturaRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final FaturaMapper faturaMapper;
    private final PagamentoFaturaMapper pagamentoFaturaMapper;
    private final LancamentoMapper lancamentoMapper;
    private final CorrecaoFaturaMapper correcaoFaturaMapper;

    @Transactional(readOnly = true)
    public List<CorrecaoFaturaHistoricoResponseDTO> listarHistorico(Long faturaId) {
        if (!faturaRepository.existsById(faturaId)) {
            throw new EntityNotFoundException("Fatura não encontrada.");
        }
        return correcaoFaturaRepository.findAllByFaturaIdOrderByDataHoraCorrecaoDescIdDesc(faturaId)
                .stream()
                .map(correcaoFaturaMapper::toHistoricoResponseDTO)
                .toList();
    }

    @Transactional
    public CorrecaoFaturaResponseDTO corrigirFaturaPaga(
            Long faturaId,
            CorrigirFaturaPagaRequestDTO dto,
            String idempotencyKeyHeader) {

        String chaveIdempotencia = resolverEValidarChaveIdempotencia(dto.chaveIdempotencia(), idempotencyKeyHeader);

        if (correcaoFaturaRepository.existsByChaveIdempotencia(chaveIdempotencia)) {
            throw new ConflitoNegocioException(
                    "Esta correção já foi processada anteriormente (chave de idempotência duplicada).");
        }

        // Lock pessimista para serializar alterações simultâneas sobre a mesma fatura
        Fatura fatura = faturaRepository.findByIdForUpdate(faturaId)
                .orElseThrow(() -> new EntityNotFoundException("Fatura não encontrada."));

        // Validação opcional de versão otimista enviada pelo cliente
        if (dto.versaoFatura() != null && !dto.versaoFatura().equals(fatura.getVersao())) {
            throw new ObjectOptimisticLockingFailureException(Fatura.class, faturaId);
        }

        // Double-check após lock para impedir processamento duplicado em requisições concorrentes
        if (correcaoFaturaRepository.existsByChaveIdempotencia(chaveIdempotencia)) {
            throw new ConflitoNegocioException(
                    "Esta correção já foi processada anteriormente (chave de idempotência duplicada).");
        }

        fatura.validarPagaParaCorrecao();

        // Snapshot do estado anterior da fatura para histórico/auditoria
        BigDecimal valorTotalAnterior = valorSeguro(fatura.getValorTotal());
        BigDecimal valorPagoAnterior = valorSeguro(fatura.getValorPago());
        BigDecimal saldoRestanteAnterior = fatura.getSaldoRestante();
        StatusFatura statusAnterior = fatura.getStatus();

        // 1. Cadastrar a despesa esquecida diretamente na fatura original (sem deslocar para outra fatura)
        CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO despesaDto = dto.despesa();
        BigDecimal valorDespesaValidado = validarENormalizarValorMonetario(
                despesaDto.valor(), "O valor da despesa");
        Lancamento lancamentoSalvo = criarDespesaEsquecidaNaFatura(fatura, despesaDto, valorDespesaValidado);

        // 2. Quando informado, corrigir o pagamento existente (nunca cria pagamentos automaticamente)
        PagamentoFatura pagamentoCorrigido = null;
        Lancamento lancamentoPagamento = null;
        Long contaAnteriorId = null;
        Long contaNovaId = null;
        LocalDate dataPagamentoAnterior = null;
        LocalDate dataPagamentoNova = null;
        BigDecimal valorPagamentoAnterior = null;
        BigDecimal valorPagamentoNovo = null;

        if (dto.correcaoPagamento() != null) {
            CorrigirFaturaPagaRequestDTO.CorrecaoPagamentoDTO cpDto = dto.correcaoPagamento();

            PagamentoFatura pagamento = pagamentoFaturaRepository
                    .findByIdAndFaturaIdForUpdate(cpDto.pagamentoFaturaId(), faturaId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Registro de pagamento não encontrado para esta fatura."));

            contaAnteriorId = pagamento.getConta().getId();
            dataPagamentoAnterior = pagamento.getDataPagamento();
            valorPagamentoAnterior = valorSeguro(pagamento.getValor());

            contaNovaId = (cpDto.contaId() != null) ? cpDto.contaId() : contaAnteriorId;
            dataPagamentoNova = (cpDto.dataPagamento() != null) ? cpDto.dataPagamento() : dataPagamentoAnterior;
            valorPagamentoNovo = (cpDto.novoValor() != null)
                    ? validarENormalizarValorMonetario(cpDto.novoValor(), "O novo valor do pagamento")
                    : valorPagamentoAnterior;

            Conta contaFinal = ajustarSaldosContas(
                    contaAnteriorId,
                    contaNovaId,
                    valorPagamentoAnterior,
                    valorPagamentoNovo
            );

            pagamento.setConta(contaFinal);
            pagamento.setValor(valorPagamentoNovo);
            pagamento.setDataPagamento(dataPagamentoNova);
            if (cpDto.observacoes() != null) {
                pagamento.setObservacoes(cpDto.observacoes());
            }

            pagamentoCorrigido = pagamentoFaturaRepository.saveAndFlush(pagamento);

            lancamentoPagamento = lancamentoRepository.findByPagamentoFaturaId(pagamentoCorrigido.getId())
                    .orElse(null);
            if (lancamentoPagamento != null) {
                lancamentoPagamento.setValor(valorPagamentoNovo);
                lancamentoPagamento.setConta(contaFinal);
                lancamentoPagamento.setDataLancamento(dataPagamentoNova);
                lancamentoPagamento.setDataVencimento(dataPagamentoNova);
                lancamentoPagamento.setDataEfetivacao(dataPagamentoNova);
                lancamentoPagamento.setObservacoes(pagamentoCorrigido.getObservacoes());
            }
        }

        // 3. Recalcular total da fatura, pagamentos, saldo restante e status
        BigDecimal novoValorTotal = valorSeguro(lancamentoRepository.somarValorPorFaturaId(fatura.getId()));
        BigDecimal novoValorPago = valorSeguro(pagamentoFaturaRepository.somarValorPorFaturaId(fatura.getId()));
        fatura.aplicarRecalculoCorrecao(novoValorTotal, novoValorPago);

        if (lancamentoPagamento != null && pagamentoCorrigido != null) {
            lancamentoPagamento.setDescricao(montarDescricaoPagamentoFatura(pagamentoCorrigido, fatura));
            lancamentoRepository.save(lancamentoPagamento);
        }

        // 4. Recalcular status e data de efetivação dos lançamentos da fatura
        atualizarStatusLancamentosDaFatura(fatura, lancamentoSalvo);

        // 5. Persistir fatura atualizada e recalcular limite disponível do cartão
        Fatura faturaSalva = faturaRepository.saveAndFlush(fatura);
        CartaoCredito cartao = faturaSalva.getCartao();
        BigDecimal limiteUtilizado = valorSeguro(faturaRepository.somarLimiteUtilizadoPorCartao(cartao.getId()));
        BigDecimal limiteDisponivel = cartao.calcularLimiteDisponivel(limiteUtilizado);

        // 6. Registrar histórico da correção (com chave de idempotência única)
        CorrecaoFatura correcao = new CorrecaoFatura();
        correcao.setFatura(faturaSalva);
        correcao.setChaveIdempotencia(chaveIdempotencia);
        correcao.setLancamentoId(lancamentoSalvo.getId());
        correcao.setDescricaoDespesa(lancamentoSalvo.getDescricao());
        correcao.setValorDespesa(lancamentoSalvo.getValor());
        correcao.setDataCompra(lancamentoSalvo.getDataLancamento());
        correcao.setPagamentoCorrigido(pagamentoCorrigido != null);
        correcao.setPagamentoFaturaId(pagamentoCorrigido != null ? pagamentoCorrigido.getId() : null);
        correcao.setContaAnteriorId(contaAnteriorId);
        correcao.setContaNovaId(contaNovaId);
        correcao.setDataPagamentoAnterior(dataPagamentoAnterior);
        correcao.setDataPagamentoNova(dataPagamentoNova);
        correcao.setValorPagamentoAnterior(valorPagamentoAnterior);
        correcao.setValorPagamentoNovo(valorPagamentoNovo);
        correcao.setValorTotalAnterior(valorTotalAnterior);
        correcao.setValorTotalNovo(faturaSalva.getValorTotal());
        correcao.setValorPagoAnterior(valorPagoAnterior);
        correcao.setValorPagoNovo(faturaSalva.getValorPago());
        correcao.setSaldoRestanteAnterior(saldoRestanteAnterior);
        correcao.setSaldoRestanteNovo(faturaSalva.getSaldoRestante());
        correcao.setStatusAnterior(statusAnterior);
        correcao.setStatusNovo(faturaSalva.getStatus());
        correcao.setMotivo(dto.motivo());
        correcao.setDataHoraCorrecao(LocalDateTime.now());

        CorrecaoFatura correcaoSalva = correcaoFaturaRepository.saveAndFlush(correcao);

        return new CorrecaoFaturaResponseDTO(
                correcaoFaturaMapper.toHistoricoResponseDTO(correcaoSalva),
                faturaMapper.toResponseDTO(faturaSalva),
                lancamentoMapper.toResponseDTO(lancamentoSalvo),
                pagamentoCorrigido != null ? pagamentoFaturaMapper.toResponseDTO(pagamentoCorrigido) : null,
                limiteUtilizado,
                limiteDisponivel
        );
    }

    private String resolverEValidarChaveIdempotencia(String chaveBody, String chaveHeader) {
        String bodyLimpa = (chaveBody != null && !chaveBody.isBlank()) ? chaveBody.trim() : null;
        String headerLimpa = (chaveHeader != null && !chaveHeader.isBlank()) ? chaveHeader.trim() : null;

        if (bodyLimpa != null && headerLimpa != null && !bodyLimpa.equals(headerLimpa)) {
            throw new RegraNegocioException(
                    "A chave de idempotência informada no cabeçalho diverge da chave informada no corpo da requisição.");
        }

        String chaveFinal = (headerLimpa != null) ? headerLimpa : bodyLimpa;
        if (chaveFinal == null) {
            throw new RegraNegocioException(
                    "A chave de idempotência é obrigatória. Informe via cabeçalho 'Idempotency-Key' ou campo 'chaveIdempotencia'.");
        }
        if (chaveFinal.length() > 100) {
            throw new RegraNegocioException("A chave de idempotência deve ter no máximo 100 caracteres.");
        }
        return chaveFinal;
    }

    private Lancamento criarDespesaEsquecidaNaFatura(
            Fatura fatura,
            CorrigirFaturaPagaRequestDTO.DespesaEsquecidaDTO despesaDto,
            BigDecimal valorValidado) {

        Categoria categoria = categoriaRepository.findById(despesaDto.categoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
        if (!categoria.isAtivo()) {
            throw new RegraNegocioException("Não é possível realizar lançamentos em uma categoria inativa.");
        }

        Subcategoria subcategoria = null;
        if (despesaDto.subcategoriaId() != null) {
            subcategoria = subcategoriaRepository.findById(despesaDto.subcategoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada."));
            if (!subcategoria.isAtivo()) {
                throw new RegraNegocioException("Não é possível realizar lançamentos em uma subcategoria inativa.");
            }
            if (!subcategoria.getCategoria().getId().equals(categoria.getId())) {
                throw new RegraNegocioException("A Subcategoria informada não pertence à Categoria selecionada.");
            }
        }

        Lancamento lancamento = new Lancamento();
        lancamento.setTipo(TipoLancamento.DESPESA);
        lancamento.setDescricao(despesaDto.descricao().trim());
        lancamento.setValor(valorValidado);
        lancamento.setConta(null);
        lancamento.setContaDestino(null);
        lancamento.setCategoria(categoria);
        lancamento.setSubcategoria(subcategoria);
        lancamento.setDataLancamento(despesaDto.dataLancamento());
        lancamento.setDataVencimento(fatura.getDataVencimento());
        lancamento.setDataEfetivacao(null);
        lancamento.setStatus(StatusLancamento.PENDENTE);
        lancamento.setObservacoes(despesaDto.observacoes());
        lancamento.setTipoRecorrencia(TipoRecorrencia.NENHUMA);
        lancamento.setParcelaAtual(1);
        lancamento.setTotalParcelas(1);
        lancamento.setGrupoParcelamentoId(null);
        lancamento.setCartaoCreditoId(fatura.getCartao().getId());
        lancamento.setFaturaId(fatura.getId());
        lancamento.setDocumentoFiscalId(despesaDto.documentoFiscalId());
        lancamento.setPagamentoFaturaId(null);

        return lancamentoRepository.saveAndFlush(lancamento);
    }

    private Conta ajustarSaldosContas(
            Long contaAnteriorId,
            Long contaNovaId,
            BigDecimal valorAnterior,
            BigDecimal valorNovo) {

        if (contaAnteriorId.equals(contaNovaId)) {
            Conta conta = contaRepository.findByIdForUpdate(contaAnteriorId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta bancária do pagamento não encontrada."));

            BigDecimal diferenca = valorNovo.subtract(valorAnterior);
            if (diferenca.compareTo(BigDecimal.ZERO) > 0) {
                conta.debitar(diferenca);
            } else if (diferenca.compareTo(BigDecimal.ZERO) < 0) {
                conta.creditar(diferenca.abs());
            }
            return contaRepository.save(conta);
        }

        // Adquire locks em ordem crescente de ID para evitar deadlocks entre transações concorrentes
        Long primeiroId = Math.min(contaAnteriorId, contaNovaId);
        Long segundoId = Math.max(contaAnteriorId, contaNovaId);

        Conta primeiraConta = contaRepository.findByIdForUpdate(primeiroId)
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária não encontrada."));
        Conta segundaConta = contaRepository.findByIdForUpdate(segundoId)
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária não encontrada."));

        Conta contaAnterior = contaAnteriorId.equals(primeiroId) ? primeiraConta : segundaConta;
        Conta contaNova = contaNovaId.equals(primeiroId) ? primeiraConta : segundaConta;

        if (!contaNova.isAtivo()) {
            throw new RegraNegocioException(
                    "Não é possível corrigir o pagamento utilizando uma conta inativa.");
        }

        contaAnterior.creditar(valorAnterior);
        contaNova.debitar(valorNovo);

        contaRepository.save(contaAnterior);
        return contaRepository.save(contaNova);
    }

    private void atualizarStatusLancamentosDaFatura(Fatura fatura, Lancamento lancamentoSalvo) {
        StatusLancamento novoStatus;
        LocalDate dataEfetivacao;

        if (fatura.getStatus() == StatusFatura.PAGA) {
            novoStatus = StatusLancamento.EFETIVADO;
            dataEfetivacao = pagamentoFaturaRepository
                    .findFirstByFaturaIdOrderByDataPagamentoDescIdDesc(fatura.getId())
                    .map(PagamentoFatura::getDataPagamento)
                    .orElse(LocalDate.now());
        } else {
            novoStatus = StatusLancamento.PENDENTE;
            dataEfetivacao = null;
        }

        lancamentoSalvo.setStatus(novoStatus);
        lancamentoSalvo.setDataEfetivacao(dataEfetivacao);

        List<Lancamento> lancamentos = lancamentoRepository.findAllByFaturaId(fatura.getId());
        if (!lancamentos.isEmpty()) {
            for (Lancamento lancamento : lancamentos) {
                lancamento.setStatus(novoStatus);
                lancamento.setDataEfetivacao(dataEfetivacao);
            }
            lancamentoRepository.saveAll(lancamentos);
        } else {
            lancamentoRepository.save(lancamentoSalvo);
        }
    }

    private String montarDescricaoPagamentoFatura(PagamentoFatura pagamento, Fatura fatura) {
        boolean ehParcial = pagamento.getValor().compareTo(fatura.getValorTotal()) < 0;
        String prefixo = ehParcial
                ? (fatura.getSaldoRestante().compareTo(BigDecimal.ZERO) > 0
                        ? "Pagamento parcial fatura "
                        : "Pagamento complementar fatura ")
                : "Pagamento fatura ";

        String descricao = prefixo + fatura.getMesAno() + " - " + fatura.getCartao().getNome();
        return descricao.length() > 100 ? descricao.substring(0, 100) : descricao;
    }

    private BigDecimal validarENormalizarValorMonetario(BigDecimal valor, String nomeCampo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException(nomeCampo + " deve ser maior que zero.");
        }
        int casasDecimais = Math.max(valor.scale(), 0);
        int digitosInteiros = valor.precision() - valor.scale();
        if (casasDecimais > 2 || digitosInteiros > 17) {
            throw new RegraNegocioException(
                    nomeCampo + " deve ter no máximo 17 dígitos inteiros e 2 casas decimais.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return (valor != null) ? valor : BigDecimal.ZERO;
    }
}
