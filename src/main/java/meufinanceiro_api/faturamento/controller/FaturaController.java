package meufinanceiro_api.faturamento.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.faturamento.dto.CorrecaoFaturaHistoricoResponseDTO;
import meufinanceiro_api.faturamento.dto.CorrecaoFaturaResponseDTO;
import meufinanceiro_api.faturamento.dto.CorrigirFaturaPagaRequestDTO;
import meufinanceiro_api.faturamento.dto.FaturaResponseDTO;
import meufinanceiro_api.faturamento.dto.PagamentoFaturaRequestDTO;
import meufinanceiro_api.faturamento.dto.PagamentoFaturaResponseDTO;
import meufinanceiro_api.faturamento.enums.StatusFatura;
import meufinanceiro_api.faturamento.service.CorrecaoFaturaService;
import meufinanceiro_api.faturamento.service.FaturaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Faturas", description = "Endpoints para consulta e gestão do ciclo de vida de faturas de cartão de crédito")
@RestController
@RequestMapping("/api/faturas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FaturaController {

    private final FaturaService faturaService;
    private final CorrecaoFaturaService correcaoFaturaService;

    @Operation(summary = "Listar faturas", description = "Lista as faturas ordenadas por data de vencimento decrescente, permitindo filtrar por cartão, competência (MM/yyyy) e status.")
    @GetMapping
    public List<FaturaResponseDTO> listar(
            @RequestParam(required = false) Long cartaoCreditoId,
            @RequestParam(required = false) String mesAno,
            @RequestParam(required = false) StatusFatura status) {
        return faturaService.listar(cartaoCreditoId, mesAno, status);
    }

    @Operation(summary = "Buscar fatura por ID", description = "Retorna os detalhes e valores consolidados de uma fatura específica.")
    @GetMapping("/{id}")
    public FaturaResponseDTO buscarPorId(@PathVariable Long id) {
        return faturaService.buscarPorId(id);
    }

    @Operation(summary = "Fechar fatura", description = "Altera o status de uma fatura ABERTA para FECHADA (ou para PAGA caso todo o saldo já tenha sido adiantado), bloqueando a inclusão de novos lançamentos.")
    @PatchMapping("/{id}/fechar")
    public FaturaResponseDTO fechar(@PathVariable Long id) {
        return faturaService.fecharFatura(id);
    }

    @Operation(summary = "Reabrir fatura", description = "Reabre uma fatura FECHADA para permitir novas movimentações, voltando seu status para ABERTA.")
    @PatchMapping("/{id}/reabrir")
    public FaturaResponseDTO reabrir(@PathVariable Long id) {
        return faturaService.reabrirFatura(id);
    }

    @Operation(summary = "Pagar fatura (total, parcial ou adiantamento)", description = "Registra um pagamento na fatura (mesmo que ABERTA ou parcial), debitando o saldo da conta informada (ou da conta padrão do cartão) e liberando o limite imediatamente. Quando o saldo restante zerar, a fatura passa automaticamente para PAGA e efetiva seus lançamentos.")
    @PostMapping("/{id}/pagar")
    public FaturaResponseDTO pagar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PagamentoFaturaRequestDTO dto) {
        return faturaService.pagarFatura(id, dto);
    }

    @Operation(summary = "Listar histórico de pagamentos da fatura", description = "Retorna todos os pagamentos (parciais, adiantamentos ou quitação) realizados para a fatura informada.")
    @GetMapping("/{id}/pagamentos")
    public List<PagamentoFaturaResponseDTO> listarPagamentos(@PathVariable Long id) {
        return faturaService.listarPagamentos(id);
    }

    @Operation(summary = "Estornar pagamento da fatura", description = "Estorna um pagamento específico da fatura, devolvendo o valor para a conta bancária de origem. Caso a fatura estivesse PAGA, retorna seu status para FECHADA e seus lançamentos para PENDENTE.")
    @DeleteMapping("/{id}/pagamentos/{pagamentoId}")
    public FaturaResponseDTO estornarPagamento(
            @PathVariable Long id,
            @PathVariable Long pagamentoId) {
        return faturaService.estornarPagamento(id, pagamentoId);
    }

    @Operation(
            summary = "Corrigir fatura paga",
            description = "Executa em uma única transação a inclusão de uma despesa esquecida diretamente na fatura paga original (sem deslocar para outra fatura e sem criar pagamentos automaticamente). Permite opcionalmente corrigir um pagamento existente (valor, conta e data real), recalculando total da fatura, pagamentos, saldo restante, limite disponível do cartão, status dos lançamentos e registrando o histórico da correção com proteção contra duplicidade e concorrência."
    )
    @PostMapping("/{id}/correcoes")
    public CorrecaoFaturaResponseDTO corrigirFaturaPaga(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CorrigirFaturaPagaRequestDTO dto) {
        return correcaoFaturaService.corrigirFaturaPaga(id, dto, idempotencyKey);
    }

    @Operation(
            summary = "Listar histórico de correções da fatura",
            description = "Retorna o histórico auditável de todas as correções aplicadas à fatura informada."
    )
    @GetMapping("/{id}/correcoes")
    public List<CorrecaoFaturaHistoricoResponseDTO> listarCorrecoes(@PathVariable Long id) {
        return correcaoFaturaService.listarHistorico(id);
    }
}

