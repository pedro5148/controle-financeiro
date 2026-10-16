package meufinanceiro_api.financeiro.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.dto.ContaRequestDTO;
import meufinanceiro_api.financeiro.dto.ContaResponseDTO;
import meufinanceiro_api.financeiro.dto.ContaResumoDTO;
import meufinanceiro_api.financeiro.service.ContaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/contas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService service;

    @GetMapping
    public List<ContaResponseDTO> listarTodasAsContas(@RequestParam(required = false) Boolean apenasAtivas) {
        return service.listarTodasAsContas(apenasAtivas);
    }

    @GetMapping("/resumo")
    public ContaResumoDTO obterResumo() {
        return service.obterResumoContas();
    }

    @GetMapping("/saldo-total")
    public ContaResumoDTO obterSaldoTotal() {
        return service.obterResumoContas();
    }

    @GetMapping("/{id}")
    public ContaResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ContaResponseDTO> criar(@Valid @RequestBody ContaRequestDTO dto) {
        ContaResponseDTO resposta = service.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @PutMapping("/{id}")
    public ContaResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody ContaRequestDTO dto) {
        return service.atualizar(id, dto);
    }

    @PatchMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @PatchMapping("/{id}/ativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ativar(@PathVariable Long id) {
        service.ativar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
