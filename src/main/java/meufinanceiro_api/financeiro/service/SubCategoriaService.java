package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.domain.Subcategoria;
import meufinanceiro_api.financeiro.dto.SubcategoriaRequestDTO;
import meufinanceiro_api.financeiro.dto.SubcategoriaResponseDTO;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.SubcategoriaMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import meufinanceiro_api.financeiro.repository.SubcategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubCategoriaService {

    private final SubcategoriaRepository subcategoriaRepository;
    private final CategoriaRepository categoriaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final SubcategoriaMapper subcategoriaMapper;

    @Transactional
    public SubcategoriaResponseDTO criar(SubcategoriaRequestDTO dto) {
        Categoria categoriaPai = categoriaRepository.findById(dto.categoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria pai não encontrada."));

        String nomeFormatado = dto.nome().trim();

        if (subcategoriaRepository.existsByCategoriaIdAndNomeIgnoreCase(categoriaPai.getId(), nomeFormatado)) {
            throw new RegraNegocioException("Esta categoria já possui uma subcategoria com este nome.");
        }

        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setNome(nomeFormatado);
        subcategoria.setCategoria(categoriaPai);

        return subcategoriaMapper.toResponseDTO(subcategoriaRepository.save(subcategoria));
    }

    @Transactional
    public SubcategoriaResponseDTO atualizar(Long id, SubcategoriaRequestDTO dto) {
        Subcategoria existente = subcategoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada."));

        Categoria novaCategoriaPai = categoriaRepository.findById(dto.categoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria pai não encontrada."));

        String novoNomeFormatado = dto.nome().trim();

        boolean mudouNome = !existente.getNome().equalsIgnoreCase(novoNomeFormatado);
        boolean mudouCategoria = !existente.getCategoria().getId().equals(novaCategoriaPai.getId());

        if ((mudouNome || mudouCategoria) &&
                subcategoriaRepository.existsByCategoriaIdAndNomeIgnoreCase(novaCategoriaPai.getId(), novoNomeFormatado)) {
            throw new RegraNegocioException("A categoria de destino já possui uma subcategoria com este nome.");
        }

        existente.setNome(novoNomeFormatado);
        existente.setCategoria(novaCategoriaPai);

        return subcategoriaMapper.toResponseDTO(subcategoriaRepository.save(existente));
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
