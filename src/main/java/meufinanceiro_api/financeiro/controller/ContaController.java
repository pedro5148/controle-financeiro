package meufinanceiro_api.financeiro.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.dto.ContaRequestDTO;
import meufinanceiro_api.financeiro.dto.ContaResponseDTO;
import meufinanceiro_api.financeiro.dto.ContaResumoDTO;
import meufinanceiro_api.financeiro.dto.ReajusteSaldoRequestDTO;
import meufinanceiro_api.financeiro.dto.SaldoMensalResponseDTO;
import meufinanceiro_api.financeiro.service.ContaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Contas", description = "Endpoints para gerenciamento de contas bancárias e carteiras")
@RestController
@RequestMapping("/api/contas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService service;

    @Operation(summary = "Listar contas", description = "Retorna a lista de contas cadastradas ordenadas com a conta padrão no topo. Permite filtrar apenas ativas.")
    @GetMapping
    public List<ContaResponseDTO> listarTodasAsContas(@RequestParam(required = false) Boolean apenasAtivas) {
        return service.listarTodasAsContas(apenasAtivas);
    }

    @Operation(summary = "Resumo financeiro e saldo consolidado", description = "Retorna a somatória do saldo atual de todas as contas ativas e os contadores de contas.")
    @GetMapping("/resumo")
    public ContaResumoDTO obterResumo() {
        return service.obterResumoContas();
    }

    @Operation(summary = "Saldos mensais (Visão Geral / Dashboard)", description = "Calcula o saldo inicial do mês, o saldo atual e o saldo previsto no fim do mês selecionado, permitindo filtrar por uma conta específica ou consolidar todas as contas ativas.")
    @GetMapping("/saldos-mensais")
    public SaldoMensalResponseDTO obterSaldosMensais(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Long contaId) {
        return service.obterSaldosMensais(mes, ano, contaId);
    }

    @Operation(summary = "Buscar conta por ID", description = "Retorna os detalhes de uma conta específica informada pelo identificador único.")
    @GetMapping("/{id}")
    public ContaResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Criar nova conta", description = "Cadastra uma nova conta. Se for a primeira conta do sistema, ela é automaticamente definida como padrão.")
    @PostMapping
    public ResponseEntity<ContaResponseDTO> criar(@Valid @RequestBody ContaRequestDTO dto) {
        ContaResponseDTO resposta = service.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @Operation(summary = "Atualizar conta", description = "Atualiza os dados da conta (nome, saldo inicial, status padrão ou status ativo) e recalcula o saldo atual com base nos lançamentos efetivados.")
    @PutMapping("/{id}")
    public ContaResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody ContaRequestDTO dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Reajustar saldo atual", description = "Reajusta o saldo atual da conta para o valor informado, gerando automaticamente um lançamento efetivado de ajuste (RECEITA ou DESPESA) caso haja diferença.")
    @PatchMapping("/{id}/reajustar-saldo")
    public ContaResponseDTO reajustarSaldoAtual(@PathVariable Long id, @Valid @RequestBody ReajusteSaldoRequestDTO dto) {
        return service.reajustarSaldoAtual(id, dto);
    }

    @Operation(summary = "Inativar conta", description = "Desativa a conta para que não possa ser mais selecionada em novos lançamentos financeiros.")
    @PatchMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @Operation(summary = "Ativar conta", description = "Reativa uma conta previamente inativada.")
    @PatchMapping("/{id}/ativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ativar(@PathVariable Long id) {
        service.ativar(id);
    }

    @Operation(summary = "Excluir conta", description = "Remove fisicamente a conta caso ela não possua lançamentos financeiros atrelados e não seja a conta padrão.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
