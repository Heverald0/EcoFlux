package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.AlterarUsuarioRequisicao;
import com.example.EcoFlux.dto.request.AnalisarInstituicaoRequisicao;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;
import com.example.EcoFlux.dto.request.response.AdministracaoUsuarioResposta;
import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.*;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.exception.ExcecaoRegraNegocio;
import com.example.EcoFlux.repository.InstituicaoRepositorio;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Transactional(readOnly = true)
public class AdministracaoServico {
    private final UsuarioRepositorio usuarios;
    private final InstituicaoRepositorio instituicoes;
    private final EventoServico eventos;

    public Page<AdministracaoUsuarioResposta> usuarios(String busca, StatusUsuario status, PerfilUsuario perfil, int pagina, int tamanho) {
        Specification<Usuario> filtro = (root, query, cb) -> cb.conjunction();
        if (busca != null && !busca.isBlank()) {
            String termo = "%" + busca.trim().toLowerCase(Locale.ROOT) + "%";
            filtro = filtro.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("nome")), termo), cb.like(cb.lower(root.get("email")), termo)));
        }
        if (status != null) filtro = filtro.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (perfil != null) filtro = filtro.and((root, query, cb) -> cb.equal(root.get("perfil"), perfil));
        Page<Usuario> resultado = usuarios.findAll(filtro, paginar(pagina, tamanho));
        Map<Long, List<Instituicao>> vinculos = resultado.isEmpty() ? Map.of()
                : instituicoes.findAllByUsuarioResponsavelIdIn(resultado.stream().map(Usuario::getId).toList()).stream()
                .collect(Collectors.groupingBy(i -> i.getUsuarioResponsavel().getId()));
        return resultado.map(u -> AdministracaoUsuarioResposta.de(u, vinculos.getOrDefault(u.getId(), List.of())));
    }

    public Page<InstituicaoResposta> instituicoes(String busca, StatusInstituicao status, TipoInstituicao tipo, int pagina, int tamanho) {
        Specification<Instituicao> filtro = (root, query, cb) -> {
            if (query.getResultType() != Long.class) root.fetch("usuarioResponsavel");
            return cb.conjunction();
        };
        if (busca != null && !busca.isBlank()) {
            String termo = "%" + busca.trim().toLowerCase(Locale.ROOT) + "%";
            filtro = filtro.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), termo));
        }
        if (status != null) filtro = filtro.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (tipo != null) filtro = filtro.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo));
        return instituicoes.findAll(filtro, paginar(pagina, tamanho)).map(InstituicaoResposta::converter);
    }

    @Transactional
    public UsuarioResposta alterarUsuario(Long id, AlterarUsuarioRequisicao requisicao, Usuario autor) {
        usuarios.buscarParaAtualizar(Math.min(id, autor.getId())).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Usuario nao encontrado."));
        usuarios.buscarParaAtualizar(Math.max(id, autor.getId())).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Usuario nao encontrado."));
        Usuario administrador = exigirAdministrador(autor.getId());
        Usuario usuario = usuarios.findById(id).orElseThrow();
        if (requisicao.status() == null && requisicao.perfil() == null) throw new ExcecaoRegraNegocio("Informe status ou perfil.");
        if (id.equals(autor.getId()) && ((requisicao.status() != null && requisicao.status() != StatusUsuario.ATIVO)
                || (requisicao.perfil() != null && requisicao.perfil() != PerfilUsuario.ADMINISTRADOR))) {
            throw new ExcecaoRegraNegocio("O administrador nao pode remover o proprio acesso.");
        }
        String anterior = usuario.getPerfil() + "/" + usuario.getStatus();
        if (requisicao.status() != null) usuario.setStatus(requisicao.status());
        if (requisicao.perfil() != null) usuario.setPerfil(requisicao.perfil());
        usuario.setVersaoToken(usuario.getVersaoToken() + 1);
        usuarios.save(usuario);
        eventos.registrar(UUID.randomUUID().toString(), "USUARIO_ALTERADO", "USUARIO", id, administrador, usuario, null,
                anterior + " -> " + usuario.getPerfil() + "/" + usuario.getStatus() + ": " + requisicao.motivo().trim());
        return UsuarioResposta.aPartirDaEntidade(usuario);
    }

    @Transactional
    public InstituicaoResposta analisarInstituicao(Long id, AnalisarInstituicaoRequisicao requisicao, Usuario autor) {
        Usuario administrador = exigirAdministrador(autor.getId());
        Instituicao instituicao = instituicoes.buscarParaAtualizar(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Instituicao nao encontrada."));
        StatusInstituicao anterior = instituicao.getStatus();
        if (anterior == requisicao.status()) return InstituicaoResposta.converter(instituicao);
        instituicao.setStatus(requisicao.status());
        instituicao.setMotivoStatus(requisicao.motivo().trim());
        instituicao.setDataAnalise(LocalDateTime.now());
        instituicao.setAdministradorAnalise(administrador);
        Usuario responsavel = instituicao.getUsuarioResponsavel();
        if (requisicao.status() == StatusInstituicao.ATIVO && instituicao.getTipo() == TipoInstituicao.COOPERATIVA
                && (responsavel.getPerfil() == PerfilUsuario.INSTITUICAO || responsavel.getPerfil() == PerfilUsuario.USUARIO)) {
            responsavel.setPerfil(PerfilUsuario.COOPERATIVA);
            responsavel.setVersaoToken(responsavel.getVersaoToken() + 1);
            usuarios.save(responsavel);
        }
        instituicoes.save(instituicao);
        eventos.registrar(UUID.randomUUID().toString(), "INSTITUICAO_ANALISADA", "INSTITUICAO", id, administrador,
                responsavel, instituicao, anterior + " -> " + requisicao.status() + ": " + requisicao.motivo().trim());
        return InstituicaoResposta.converter(instituicao);
    }

    public Map<String, Long> resumo() {
        return Map.of("usuariosAtivos", usuarios.countByStatus(StatusUsuario.ATIVO), "instituicoes", instituicoes.count(),
                "cooperativas", instituicoes.countByTipo(TipoInstituicao.COOPERATIVA),
                "aprovacoesPendentes", instituicoes.countByStatus(StatusInstituicao.PENDENTE),
                "perfis", (long) PerfilUsuario.values().length);
    }

    public Map<PerfilUsuario, List<String>> permissoes() {
        return Map.of(
                PerfilUsuario.USUARIO, List.of("perfil", "coletas.proprias", "pontos", "educacao"),
                PerfilUsuario.INSTITUICAO, List.of("instituicoes.proprias", "coletas.proprias", "materiais.proprios", "indicadores.proprios", "relatorios.proprios"),
                PerfilUsuario.COOPERATIVA, List.of("instituicoes.proprias", "coletas.atribuidas", "rotas.proprias", "materiais.atribuidos"),
                PerfilUsuario.OPERADOR, List.of("coletas.operacional", "rotas.operacional", "materiais.recebimento"),
                PerfilUsuario.ADMINISTRADOR, List.of("administracao", "aprovacoes", "perfis", "auditoria", "operacional", "indicadores", "educacao.editar"));
    }

    private Usuario exigirAdministrador(Long id) {
        Usuario usuario = usuarios.findById(id).orElseThrow(() -> new AccessDeniedException("Administrador nao encontrado."));
        if (!usuario.isEnabled() || usuario.getPerfil() != PerfilUsuario.ADMINISTRADOR) throw new AccessDeniedException("Acesso administrativo necessario.");
        return usuario;
    }

    private PageRequest paginar(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) throw new ExcecaoRegraNegocio("Pagina deve ser positiva e tamanho entre 1 e 100.");
        return PageRequest.of(pagina, tamanho, Sort.by("id").descending());
    }
}
