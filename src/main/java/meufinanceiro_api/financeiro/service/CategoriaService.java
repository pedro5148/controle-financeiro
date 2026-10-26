package meufinanceiro_api.financeiro.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import meufinanceiro_api.financeiro.domain.Categoria;
import meufinanceiro_api.financeiro.domain.Subcategoria;
import meufinanceiro_api.financeiro.dto.CategoriaRequestDTO;
import meufinanceiro_api.financeiro.dto.CategoriaResponseDTO;
import meufinanceiro_api.financeiro.dto.CategoriaUpdateDTO;
import meufinanceiro_api.financeiro.enums.CategoriaSistema;
import meufinanceiro_api.financeiro.exception.RegraNegocioException;
import meufinanceiro_api.financeiro.mapper.CategoriaMapper;
import meufinanceiro_api.financeiro.repository.CategoriaRepository;
import meufinanceiro_api.financeiro.repository.LancamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listarTodasCategorias(Boolean apenasAtivas) {
        List<Categoria> categorias = Boolean.TRUE.equals(apenasAtivas)
                ? categoriaRepository.findAllByAtivoTrueOrderByNomeAsc()
                : categoriaRepository.findAllByOrderByNomeAsc();

        return categorias.stream()
                .map(categoriaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarPorId(Long id) {
        return categoriaRepository.findWithSubcategoriasById(id)
                .map(categoriaMapper::toResponseDTO)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
    }

    @Transactional
    public CategoriaResponseDTO criar(CategoriaRequestDTO dto) {
        if (categoriaRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new RegraNegocioException("Já existe uma categoria com o nome informado.");
        }

        Categoria categoria = categoriaMapper.toEntity(dto);
        categoria.setNome(dto.nome());
        categoria.setAtivo(true);

        if (dto.subcategoriaNome() != null) {
            Subcategoria subcategoria = new Subcategoria();
            subcategoria.setCategoria(categoria);
            subcategoria.setNome(dto.subcategoriaNome());
            subcategoria.setAtivo(true);
            categoria.getSubcategorias().add(subcategoria);
        }

        Categoria categoriaSalva = categoriaRepository.save(categoria);
        return categoriaMapper.toResponseDTO(categoriaSalva);
    }

    @Transactional
    public CategoriaResponseDTO atualizar(Long id, CategoriaUpdateDTO dto) {
        Categoria existente = categoriaRepository.findWithSubcategoriasById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        String novoNomeFormatado = dto.nome().trim();
        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(novoNomeFormatado, id)) {
            throw new RegraNegocioException("Já existe uma categoria com o nome informado.");
        }

        existente.setNome(novoNomeFormatado);
        return categoriaMapper.toResponseDTO(categoriaRepository.save(existente));
    }

    @Transactional
    public void inativar(Long id) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        existente.setAtivo(false);
        categoriaRepository.save(existente);
    }

    @Transactional
    public void ativar(Long id) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        existente.setAtivo(true);
        categoriaRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        if (lancamentoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Não é possível excluir: A categoria possui lançamentos vinculados.");
        }

        categoriaRepository.delete(existente);
    }

    @Transactional
    public Categoria obterOuCriarCategoriaSistema(CategoriaSistema categoriaSistema) {
        String nomePadrao = categoriaSistema.getNomePadrao();
        return categoriaRepository.findByNomeIgnoreCase(nomePadrao)
                .map(cat -> {
                    if (!cat.isAtivo()) {
                        cat.setAtivo(true);
                        return categoriaRepository.save(cat);
                    }
                    return cat;
                })
                .orElseGet(() -> {
                    Categoria nova = new Categoria();
                    nova.setNome(nomePadrao);
                    nova.setAtivo(true);
                    return categoriaRepository.save(nova);
                });
    }

    @Transactional
    public Categoria obterCategoriaAtivaOuSistema(Long categoriaId, CategoriaSistema padraoSistema) {
        if (categoriaId != null) {
            Categoria categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
            if (!categoria.isAtivo()) {
                throw new RegraNegocioException("Não é possível utilizar uma categoria inativa.");
            }
            return categoria;
        }
        return obterOuCriarCategoriaSistema(padraoSistema);
    }
}
