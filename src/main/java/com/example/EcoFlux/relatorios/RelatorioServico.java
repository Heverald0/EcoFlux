package com.example.EcoFlux.relatorios;

import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.dto.MaterialDtos;
import com.example.EcoFlux.dto.request.response.ColetaResposta;
import com.example.EcoFlux.repository.ColetaRepositorio;
import com.example.EcoFlux.service.*;
import com.example.EcoFlux.exception.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class RelatorioServico {
    private final RelatorioRepositorio relatorios;
    private final ColetaRepositorio coletas;
    private final MaterialServico materiais;
    private final IndicadoresServico indicadores;
    private final EcoScoreServico score;
    private final EscopoAmbiental escopo;
    private final EventoServico eventos;
    private final ObjectMapper mapper;
    private final RelatorioPdf pdf;
    private final EntityManager em;

    @Transactional
    public RelatorioDtos.Resposta gerar(RelatorioDtos.Gerar pedido, Usuario usuario) {
        escopo.periodo(pedido.inicio(), pedido.fim());
        Instituicao instituicao = escopo.instituicao(pedido.instituicaoId(), usuario);
        Relatorio r = new Relatorio();
        r.setNome(pedido.nome().trim()); r.setTipo(pedido.tipo()); r.setInicio(pedido.inicio()); r.setFim(pedido.fim());
        r.setInstituicaoId(pedido.instituicaoId()); r.setAutor(usuario); r.setGeradoEm(LocalDateTime.now());
        relatorios.saveAndFlush(r);
        eventos.registrar("relatorio:" + r.getId() + ":pendente", "RELATORIO_PENDENTE", "RELATORIO", r.getId(), usuario, null, instituicao, "Relatorio solicitado.");
        r.setStatus("ANALISE");
        eventos.registrar("relatorio:" + r.getId() + ":analise", "RELATORIO_ANALISE", "RELATORIO", r.getId(), usuario, null, instituicao, "Consolidacao iniciada.");
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("nome", r.getNome()); dados.put("tipo", r.getTipo()); dados.put("inicio", r.getInicio()); dados.put("fim", r.getFim());
        dados.put("geradoEm", r.getGeradoEm());
        dados.put("instituicao", instituicao == null ? escopo.administrador(usuario) ? "Todas" : "Meus registros" : instituicao.getNome());
        switch (pedido.tipo()) {
            case IMPACTO -> dados.put("resumo", indicadores.resumo(pedido.instituicaoId(), pedido.inicio(), pedido.fim(), usuario));
            case MATERIAIS -> {
                var registros = materiais.concluidos(pedido.instituicaoId(), pedido.inicio(), pedido.fim(), usuario);
                if (registros.size() > 10000) throw new ExcecaoRegraNegocio("Reduza o periodo para gerar ate 10000 registros.");
                dados.put("resumo", indicadores.resumo(pedido.instituicaoId(), pedido.inicio(), pedido.fim(), usuario));
                dados.put("registros", registros.stream().map(MaterialDtos.Resposta::de).toList());
            }
            case ECOSCORE -> {
                dados.put("score", score.consultar(pedido.instituicaoId(), pedido.inicio(), pedido.fim(), usuario));
                if (pedido.instituicaoId() != null) dados.put("certificados", score.certificados(pedido.instituicaoId(), usuario));
            }
            case COLETAS -> {
                Specification<Coleta> filtro = (root, query, cb) -> {
                    var periodo = cb.and(cb.greaterThanOrEqualTo(root.get("dataHoraAgendamento"), pedido.inicio().atStartOfDay()),
                            cb.lessThan(root.get("dataHoraAgendamento"), pedido.fim().plusDays(1).atStartOfDay()));
                    if (pedido.instituicaoId() != null) return cb.and(periodo, cb.equal(root.get("instituicao").get("id"), pedido.instituicaoId()));
                    return escopo.administrador(usuario) ? periodo : cb.and(periodo, cb.equal(root.get("usuarioSolicitante").get("id"), usuario.getId()));
                };
                var pagina = coletas.findAll(filtro, PageRequest.of(0, 10000, Sort.by("dataHoraAgendamento", "id")));
                if (pagina.hasNext()) throw new ExcecaoRegraNegocio("Reduza o periodo para gerar ate 10000 registros.");
                dados.put("total", pagina.getTotalElements());
                dados.put("registros", pagina.map(ColetaResposta::converter).getContent());
            }
        }
        try { r.setDados(mapper.writeValueAsString(dados)); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Falha ao consolidar relatorio.", ex); }
        r.setStatus("PRONTO");
        eventos.registrar("relatorio:" + r.getId() + ":pronto", "RELATORIO_PRONTO", "RELATORIO", r.getId(), usuario, usuario, instituicao, "Relatorio pronto para download: " + r.getNome());
        return RelatorioDtos.Resposta.de(r);
    }

    public Page<RelatorioDtos.Resposta> listar(Usuario usuario, Pageable pagina) {
        return relatorios.findAll(filtro(usuario), PageRequest.of(pagina.getPageNumber(), Math.min(100, pagina.getPageSize()),
                Sort.by(Sort.Direction.DESC, "geradoEm", "id"))).map(RelatorioDtos.Resposta::de);
    }

    public RelatorioDtos.Detalhe buscar(Long id, Usuario usuario) {
        Relatorio r = relatorios.findById(id).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Relatorio nao encontrado."));
        validar(r, usuario);
        try { return new RelatorioDtos.Detalhe(RelatorioDtos.Resposta.de(r), mapper.readTree(r.getDados())); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Relatorio armazenado invalido.", ex); }
    }

    @Transactional
    public byte[] baixar(Long id, Usuario usuario) {
        Relatorio r = relatorios.bloquear(id).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Relatorio nao encontrado."));
        validar(r, usuario);
        if (!"PRONTO".equals(r.getStatus())) throw new ExcecaoConflitoNegocio("Relatorio ainda indisponivel.");
        byte[] documento = pdf.gerar(r);
        r.setExportacoes(r.getExportacoes() + 1);
        eventos.registrar(UUID.randomUUID().toString(), "RELATORIO_EXPORTADO", "RELATORIO", id, usuario, null, null, "Exportacao PDF.");
        return documento;
    }

    public Map<String, Long> resumo(Usuario usuario) {
        String where = escopo.administrador(usuario) ? "" : " where r.autor.id = :usuario";
        var consulta = em.createQuery("select count(r), coalesce(sum(r.exportacoes),0) from Relatorio r" + where, Object[].class);
        if (!escopo.administrador(usuario)) consulta.setParameter("usuario", usuario.getId());
        Object[] totais = consulta.getSingleResult();
        return Map.of("relatoriosGerados", ((Number) totais[0]).longValue(), "exportacoesPdf", ((Number) totais[1]).longValue());
    }

    private Specification<Relatorio> filtro(Usuario usuario) {
        return (r, q, b) -> escopo.administrador(usuario) ? b.conjunction() : b.equal(r.get("autor").get("id"), usuario.getId());
    }

    private void validar(Relatorio r, Usuario usuario) {
        if (!escopo.administrador(usuario) && !r.getAutor().getId().equals(usuario.getId())) throw new AccessDeniedException("Relatorio de outro usuario.");
    }
}
