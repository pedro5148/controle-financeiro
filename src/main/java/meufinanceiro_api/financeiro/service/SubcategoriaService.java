package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.domain.Subcategoria;
import meufinanceiro_api.financeiro.dto.SubcategoriaRequestDTO;
import meufinanceiro_api.financeiro.dto.SubcategoriaResponseDTO;
import meufinanceiro_api.financeiro.dto.SubcategoriaUpdateDTO;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.SubcategoriaMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.SubcategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubcategoriaService {

    private final SubcategoriaRepository subcategoriaRepository;
    private final CategoriaRepository categoriaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final SubcategoriaMapper subcategoriaMapper;

    @Transactional(readOnly = true)
    public List<SubcategoriaResponseDTO> listarSubcategorias(Long categoriaId, Boolean apenasAtivas) {
        if (categoriaId != null && !categoriaRepository.existsById(categoriaId)) {
            throw new EntityNotFoundException("Categoria pai não encontrada.");
        }

        List<Subcategoria> subcategorias;
        if (categoriaId != null) {
            subcategorias = Boolean.TRUE.equals(apenasAtivas)
                    ? subcategoriaRepository.findAllByCategoriaIdAndAtivoTrueOrderByNomeAsc(categoriaId)
                    : subcategoriaRepository.findAllByCategoriaIdOrderByNomeAsc(categoriaId);
        } else {
            subcategorias = Boolean.TRUE.equals(apenasAtivas)
                    ? subcategoriaRepository.findAllByAtivoTrueOrderByNomeAsc()
                    : subcategoriaRepository.findAllByOrderByNomeAsc();
        }

        return subcategorias.stream()
                .map(subcategoriaMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubcategoriaResponseDTO buscarPorId(Long id) {
        return subcategoriaRepository.findById(id)
                .map(subcategoriaMapper::toResponseDTO)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada."));
    }

    @Transactional
    public SubcategoriaResponseDTO criar(SubcategoriaRequestDTO dto) {
        Categoria categoriaPai = categoriaRepository.findById(dto.categoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria pai não encontrada."));

        if (!categoriaPai.isAtivo()) {
            throw new RegraNegocioException("Não é possível criar uma subcategoria em uma categoria inativa.");
        }

        if (subcategoriaRepository.existsByCategoriaIdAndNomeIgnoreCase(categoriaPai.getId(), dto.nome())) {
            throw new RegraNegocioException("Esta categoria já possui uma subcategoria com este nome.");
        }

        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setNome(dto.nome());
        subcategoria.setCategoria(categoriaPai);
        subcategoria.setAtivo(true);

        return subcategoriaMapper.toResponseDTO(subcategoriaRepository.save(subcategoria));
    }

    @Transactional
    public SubcategoriaResponseDTO atualizar(Long id, SubcategoriaUpdateDTO dto) {
        Subcategoria existente = subcategoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada."));

        if (subcategoriaRepository.existsByCategoriaIdAndNomeIgnoreCaseAndIdNot(existente.getCategoria().getId(), dto.nome(), id)) {
            throw new RegraNegocioException("Esta categoria já possui uma subcategoria com este nome.");
        }

        existente.setNome(dto.nome());

        return subcategoriaMapper.toResponseDTO(subcategoriaRepository.save(existente));
    }

    @Transactional
    public void inativar(Long id) {
        Subcategoria existente = subcategoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada."));

        existente.setAtivo(false);
        subcategoriaRepository.save(existente);
    }

    @Transactional
    public void ativar(Long id) {
        Subcategoria existente = subcategoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada."));

        existente.setAtivo(true);
        subcategoriaRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        if (!subcategoriaRepository.existsById(id)) {
            throw new EntityNotFoundException("Subcategoria não encontrada.");
        }

        if (lancamentoRepository.existsBySubcategoriaId(id)) {
            throw new RegraNegocioException("Não é possível excluir: Existem lançamentos financeiros vinculados a esta subcategoria.");
        }

        subcategoriaRepository.deleteById(id);
    }
}
