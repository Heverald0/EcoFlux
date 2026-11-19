package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.AmbientalDtos;
import com.example.EcoFlux.dto.request.response.ColetaResposta;
import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServico {
    private final IndicadoresServico indicadores;
    private final EcoScoreServico scores;
    private final EscopoAmbiental escopo;
    private final EntityManager entityManager;

    public record Dashboard(AmbientalDtos.Resumo materiais, long coletasRealizadas, long pontosAtivos,
            long instituicoes, List<ColetaResposta> solicitacoesRecentes, List<AmbientalDtos.Ranking> ranking) {}

    public Dashboard consultar(Long instituicaoId, LocalDate inicio, LocalDate fim, Usuario usuario) {
        escopo.periodo(inicio, fim);
        escopo.instituicao(instituicaoId, usuario);
        String filtroColetas = instituicaoId != null ? " and c.instituicao.id = :instituicao"
                : escopo.administrador(usuario) ? "" : " and (c.usuarioSolicitante.id = :usuario or i.usuarioResponsavel.id = :usuario)";
        var queryColetas = entityManager.createQuery("select count(c) from Coleta c left join c.instituicao i where "
                + "c.status in :status and c.dataConclusao >= :inicio and c.dataConclusao < :fim" + filtroColetas, Long.class)
                .setParameter("status", List.of(StatusColeta.REALIZADA, StatusColeta.CONCLUIDA))
                .setParameter("inicio", inicio.atStartOfDay()).setParameter("fim", fim.plusDays(1).atStartOfDay());
        var queryRecentes = entityManager.createQuery("select c from Coleta c left join fetch c.instituicao i "
                + "join fetch c.usuarioSolicitante left join fetch c.cooperativa where c.dataCriacao >= :inicio "
                + "and c.dataCriacao < :fim" + filtroColetas + " order by c.dataCriacao desc, c.id desc", Coleta.class)
                .setParameter("inicio", inicio.atStartOfDay()).setParameter("fim", fim.plusDays(1).atStartOfDay()).setMaxResults(10);
        if (instituicaoId != null) {
            queryColetas.setParameter("instituicao", instituicaoId);
            queryRecentes.setParameter("instituicao", instituicaoId);
        } else if (!escopo.administrador(usuario)) {
            queryColetas.setParameter("usuario", usuario.getId());
            queryRecentes.setParameter("usuario", usuario.getId());
        }
        String filtroPontos = instituicaoId != null ? " and p.instituicao.id = :instituicao"
                : escopo.administrador(usuario) ? "" : " and (p.usuarioCadastro.id = :usuario or i.usuarioResponsavel.id = :usuario)";
        var queryPontos = entityManager.createQuery("select count(p) from PontoColeta p left join p.instituicao i "
                + "where p.status = :status" + filtroPontos, Long.class).setParameter("status", StatusPontoColeta.ATIVO);
        if (instituicaoId != null) queryPontos.setParameter("instituicao", instituicaoId);
        else if (!escopo.administrador(usuario)) queryPontos.setParameter("usuario", usuario.getId());
        String filtroInstituicoes = instituicaoId != null ? " and i.id = :instituicao"
                : escopo.administrador(usuario) ? "" : " and i.usuarioResponsavel.id = :usuario";
        var queryInstituicoes = entityManager.createQuery("select count(i) from Instituicao i where i.status = :status"
                + filtroInstituicoes, Long.class).setParameter("status", StatusInstituicao.ATIVO);
        if (instituicaoId != null) queryInstituicoes.setParameter("instituicao", instituicaoId);
        else if (!escopo.administrador(usuario)) queryInstituicoes.setParameter("usuario", usuario.getId());
        return new Dashboard(indicadores.resumo(instituicaoId, inicio, fim, usuario), queryColetas.getSingleResult(),
                queryPontos.getSingleResult(), queryInstituicoes.getSingleResult(),
                queryRecentes.getResultList().stream().map(ColetaResposta::converter).toList(), scores.ranking(inicio, fim));
    }
}
