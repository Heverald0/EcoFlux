package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.ConteudoEducativoRequisicao;
import com.example.EcoFlux.dto.request.response.ConteudoEducativoResposta;
import com.example.EcoFlux.dto.request.response.GuiaTriagemMaterialResposta;
import com.example.EcoFlux.entity.ConteudoEducativo;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.TipoMaterial;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.repository.ConteudoEducativoRepositorio;
import com.example.EcoFlux.service.ConteudoEducativoServico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConteudoEducativoServicoImpl implements ConteudoEducativoServico {

    private final ConteudoEducativoRepositorio repositorio;

    @Override
    @Transactional(readOnly = true)
    public List<ConteudoEducativoResposta> listarArtigos(String categoria) {
        List<ConteudoEducativo> lista = (categoria != null && !categoria.isBlank())
                ? repositorio.findAllAtivosPorCategoriaOuMaterial(categoria.trim())
                : repositorio.findAllByAtivoTrueOrderByDataCriacaoDesc();

        return lista.stream()
                .map(ConteudoEducativoResposta::converter)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConteudoEducativoResposta buscarPorSlug(String slug) {
        ConteudoEducativo conteudo = repositorio.findBySlugAndAtivoTrue(slug.trim())
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conteudo educativo nao encontrado: " + slug));
        return ConteudoEducativoResposta.converter(conteudo);
    }

    @Override
    public List<GuiaTriagemMaterialResposta> listarGuiaTriagem() {
        return List.of(
                criarGuia(TipoMaterial.PAPEL, "Azul",
                        List.of("Jornais, revistas, cadernos, folhas sulfite, caixas de papelão desmontadas"),
                        List.of("Papel higiênico, guardanapos sujos, papel carbono, fotografias, notas fiscais térmicas"),
                        "Mantenha seco, sem grampos metálicos grandes e desmonte caixas para otimizar espaço."),

                criarGuia(TipoMaterial.PLASTICO, "Vermelho",
                        List.of("Garrafas PET, potes de alimentos, frascos de higiene/limpeza, sacolas limpas, tampinhas"),
                        List.of("Adesivos, esponjas de louça, acrílico, cabos de panela, embalagens engorduradas"),
                        "Esvazie completamente, passe água de reuso se necessário e amasse as garrafas."),

                criarGuia(TipoMaterial.VIDRO, "Verde",
                        List.of("Garrafas de bebidas, potes de conserva, frascos de perfume e medicamentos vazios"),
                        List.of("Espelhos, lâmpadas, cerâmica, porcelana, vidro temperado de automóveis ou box"),
                        "Lave por dentro; se houver cacos, embale em papelão grosso ou garrafa PET cortada e identifique."),

                criarGuia(TipoMaterial.METAL, "Amarelo",
                        List.of("Latas de alumínio (refrigerante/cerveja), latas de aço (conservas), tampas metálicas, cobre"),
                        List.of("Clipes, grampos, esponjas de aço enferrujadas, latas de tinta ou aerossol com pressão"),
                        "Enxágue o interior das latas de alimentos e pressione as latas de alumínio."),

                criarGuia(TipoMaterial.ELETRONICO, "Laranja",
                        List.of("Celulares, carregadores, cabos, notebooks, teclados, mouses, placas eletrônicas"),
                        List.of("Lâmpadas fluorescentes inteiras ou quebradas fora de embalagem protetora"),
                        "Remova cartões SIM/memória, restaure padrões de fábrica e acondicione em caixa seca."),

                criarGuia(TipoMaterial.ORGANICO, "Marrom",
                        List.of("Cascas de frutas e legumes, borra de café, cascas de ovos, podas de jardim"),
                        List.of("Carnes, laticínios em excesso, fezes de animais domésticos, óleo de cozinha"),
                        "Ideal para compostagem doméstica ou comunitária; mantenha separado dos recicláveis secos.")
        );
    }

    @Override
    @Transactional
    public ConteudoEducativoResposta criarArtigo(ConteudoEducativoRequisicao req, Usuario adminAutenticado) {
        validarAdmin(adminAutenticado);

        String slugNormalizado = req.slug().trim().toLowerCase();
        if (repositorio.existsBySlug(slugNormalizado)) {
            throw new ExcecaoConflitoNegocio("Ja existe um conteudo educativo cadastrado com o slug: " + slugNormalizado);
        }

        ConteudoEducativo novo = ConteudoEducativo.builder()
                .slug(slugNormalizado)
                .categoria(req.categoria().trim().toUpperCase())
                .titulo(req.titulo().trim())
                .resumo(req.resumo().trim())
                .corpoTexto(req.corpoTexto().trim())
                .tempoLeituraMinutos(req.tempoLeituraMinutos() != null ? req.tempoLeituraMinutos() : 3)
                .materialRelacionado(req.materialRelacionado() != null && !req.materialRelacionado().isBlank()
                        ? req.materialRelacionado().trim().toUpperCase()
                        : "GERAL")
                .boasPraticas(juntarLista(req.boasPraticas()))
                .oQueEvitar(juntarLista(req.oQueEvitar()))
                .impactoDestaque(req.impactoDestaque() != null ? req.impactoDestaque().trim() : null)
                .ativo(true)
                .build();

        return ConteudoEducativoResposta.converter(repositorio.save(novo));
    }

    @Override
    @Transactional
    public ConteudoEducativoResposta atualizarArtigo(Long id, ConteudoEducativoRequisicao req, Usuario adminAutenticado) {
        validarAdmin(adminAutenticado);

        ConteudoEducativo existente = repositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conteudo educativo nao encontrado com ID: " + id));

        String slugNormalizado = req.slug().trim().toLowerCase();
        if (!existente.getSlug().equals(slugNormalizado) && repositorio.existsBySlug(slugNormalizado)) {
            throw new ExcecaoConflitoNegocio("Ja existe outro conteudo educativo com o slug: " + slugNormalizado);
        }

        existente.setSlug(slugNormalizado);
        existente.setCategoria(req.categoria().trim().toUpperCase());
        existente.setTitulo(req.titulo().trim());
        existente.setResumo(req.resumo().trim());
        existente.setCorpoTexto(req.corpoTexto().trim());
        if (req.tempoLeituraMinutos() != null) {
            existente.setTempoLeituraMinutos(req.tempoLeituraMinutos());
        }
        existente.setMaterialRelacionado(req.materialRelacionado() != null && !req.materialRelacionado().isBlank()
                ? req.materialRelacionado().trim().toUpperCase()
                : "GERAL");
        existente.setBoasPraticas(juntarLista(req.boasPraticas()));
        existente.setOQueEvitar(juntarLista(req.oQueEvitar()));
        existente.setImpactoDestaque(req.impactoDestaque() != null ? req.impactoDestaque().trim() : null);

        return ConteudoEducativoResposta.converter(repositorio.save(existente));
    }

    @Override
    @Transactional
    public void alterarStatusAtivo(Long id, boolean ativo, Usuario adminAutenticado) {
        validarAdmin(adminAutenticado);

        ConteudoEducativo existente = repositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conteudo educativo nao encontrado com ID: " + id));

        existente.setAtivo(ativo);
        repositorio.save(existente);
    }

    private void validarAdmin(Usuario usuario) {
        if (usuario == null || usuario.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
            throw new ExcecaoConflitoNegocio("Apenas administradores podem gerenciar conteudos educativos.");
        }
    }

    private String juntarLista(List<String> itens) {
        if (itens == null || itens.isEmpty()) {
            return null;
        }
        return String.join(";", itens.stream().map(String::trim).filter(s -> !s.isBlank()).toList());
    }

    private GuiaTriagemMaterialResposta criarGuia(
            TipoMaterial material,
            String cor,
            List<String> pode,
            List<String> naoPode,
            String preparo
    ) {
        return new GuiaTriagemMaterialResposta(
                material.name(),
                material.getNomeExibicao(),
                cor,
                material.getFatorCarbonoEvitadoKg(),
                10,
                pode,
                naoPode,
                preparo
        );
    }
}