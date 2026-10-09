package meufinanceiro_api.financeiro.controller;

import jakarta.validation.Valid;
import meufinanceiro_api.financeiro.dto.ContaRequestDTO;
import meufinanceiro_api.financeiro.dto.ContaResponseDTO;
import meufinanceiro_api.financeiro.service.ContaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contas")
@CrossOrigin(origins = "*")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service){
        this.service = service;
    }

    @GetMapping
    public List<ContaResponseDTO> listarTodasAsContas() {
        return service.listarTodasAsContas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContaResponseDTO criar(@Valid @RequestBody ContaRequestDTO dto) {
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public ContaResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody ContaRequestDTO dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
