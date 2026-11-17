package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.MaterialDtos;
import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.*;
import com.example.EcoFlux.exception.*;
import com.example.EcoFlux.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialServico {
    private final RegistroMaterialRepositorio materiais;
    private final ColetaRepositorio coletas;
    private final FatorImpactoRepositorio fatores;
    private final EscopoAmbiental escopo;
    private final EcoScoreServico score;
    private final EventoServico eventos;
    private final EntityManager entityManager;

    public MaterialDtos.Resposta cadastrar(MaterialDtos.Cadastro r, Usuario usuario) {
        if (!r.unidade().aceita(r.material())) throw new ExcecaoRegraNegocio("Unidade incompativel com o material.");
        if (r.coletaId() != null) {
            Coleta coleta = coletas.findById(r.coletaId())
                    .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Coleta nao encontrada."));
            exigirOperador(coleta, usuario);
            if (coleta.getStatus() != StatusColeta.REALIZADA && coleta.getStatus() != StatusColeta.CONCLUIDA)
                throw new ExcecaoConflitoNegocio("Somente coletas concluidas podem gerar um registro de material.");
            if (r.material() != coleta.getTipoMaterial() || r.unidade() != coleta.getUnidade()
                    || coleta.getQuantidadeReal() == null || r.quantidade().compareTo(coleta.getQuantidadeReal()) != 0
                    || (r.instituicaoId() != null && (coleta.getInstituicao() == null
                    || !r.instituicaoId().equals(coleta.getInstituicao().getId()))))
                throw new ExcecaoConflitoNegocio("Os dados do material devem corresponder a coleta concluida.");
            return MaterialDtos.Resposta.de(registrarColetaConcluida(coleta, usuario));
        }
        Instituicao instituicao = escopo.instituicao(r.instituicaoId(), usuario);
        if (instituicao != null && instituicao.getStatus() != StatusInstituicao.ATIVO)
            throw new ExcecaoConflitoNegocio("A instituicao precisa estar ativa.");
        RegistroMaterial material = RegistroMaterial.builder().tipoMaterial(r.material()).quantidade(r.quantidade())
                .unidade(r.unidade()).volume(r.volume()).origem(r.origem().trim()).destino(r.destino().trim())
                .dataRegistro(r.data()).status(StatusMaterial.AGENDADA).instituicao(instituicao)
                .usuario(instituicao == null ? usuario : instituicao.getUsuarioResponsavel()).autor(usuario).build();
        materiais.save(material);
        registrarEvento(material, usuario);
        return MaterialDtos.Resposta.de(material);
    }

    public RegistroMaterial registrarColetaConcluida(Coleta coleta, Usuario autor) {
        entityManager.lock(coleta, LockModeType.PESSIMISTIC_WRITE);
        RegistroMaterial existente = materiais.findByColetaId(coleta.getId()).orElse(null);
        if (existente != null) return existente;
        if ((coleta.getStatus() != StatusColeta.REALIZADA && coleta.getStatus() != StatusColeta.CONCLUIDA)
                || coleta.getQuantidadeReal() == null || coleta.getQuantidadeReal().signum() <= 0
                || coleta.getUnidade() == null || !coleta.getUnidade().aceita(coleta.getTipoMaterial()))
            throw new ExcecaoConflitoNegocio("A coleta deve estar concluida com quantidade real e unidade validas.");
        LocalDateTime conclusao = coleta.getDataConclusao() == null ? LocalDateTime.now() : coleta.getDataConclusao();
        RegistroMaterial material = RegistroMaterial.builder().coleta(coleta).tipoMaterial(coleta.getTipoMaterial())
                .quantidade(coleta.getQuantidadeReal()).unidade(coleta.getUnidade())
                .origem(coleta.getInstituicao() == null ? coleta.getUsuarioSolicitante().getNome() : coleta.getInstituicao().getNome())
                .destino(coleta.getCooperativa() == null ? "Recebimento EcoFlux" : coleta.getCooperativa().getNome())
                .status(StatusMaterial.COLETADO).dataRegistro(conclusao.toLocalDate()).dataEfetivacao(conclusao)
                .instituicao(coleta.getInstituicao()).usuario(coleta.getUsuarioSolicitante()).autor(autor).build();
        definirFator(material);
        materiais.save(material);
        score.creditar(material, autor);
        registrarEvento(material, autor);
        return material;
    }

    public MaterialDtos.Resposta alterarStatus(Long id, StatusMaterial status, Usuario usuario) {
        RegistroMaterial material = materiais.buscarParaAtualizar(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Registro de material nao encontrado."));
        if (material.getColeta() == null && !escopo.administrador(usuario) && usuario.getPerfil() != PerfilUsuario.OPERADOR)
            throw new AccessDeniedException("O recebimento deve ser confirmado por um operador.");
        if (material.getColeta() != null) exigirOperador(material.getColeta(), usuario);
        if (!material.getStatus().permite(status)) throw new ExcecaoConflitoNegocio("Transicao de material invalida.");
        material.setStatus(status);
        if (status == StatusMaterial.COLETADO) {
            material.setDataEfetivacao(LocalDateTime.now());
            definirFator(material);
            score.creditar(material, usuario);
        }
        registrarEvento(material, usuario);
        return MaterialDtos.Resposta.de(material);
    }

    @Transactional(readOnly = true)
    public Page<MaterialDtos.Resposta> listar(Long instituicaoId, LocalDate inicio, LocalDate fim,
            TipoMaterial tipo, StatusMaterial status, int pagina, int tamanho, Usuario usuario) {
        escopo.periodo(inicio, fim);
        Specification<RegistroMaterial> spec = filtro(instituicaoId, inicio, fim, usuario, true);
        if (tipo != null) spec = spec.and((r, q, b) -> b.equal(r.get("tipoMaterial"), tipo));
        if (status != null) spec = spec.and((r, q, b) -> b.equal(r.get("status"), status));
        return materiais.findAll(spec, PageRequest.of(pagina, Math.min(tamanho, 100),
                Sort.by(Sort.Direction.DESC, "dataRegistro", "id"))).map(MaterialDtos.Resposta::de);
    }

    @Transactional(readOnly = true)
    public List<RegistroMaterial> concluidos(Long instituicaoId, LocalDate inicio, LocalDate fim, Usuario usuario) {
        escopo.periodo(inicio, fim);
        return materiais.findAll(filtro(instituicaoId, inicio, fim, usuario, false)
                .and((r, q, b) -> r.get("status").in(StatusMaterial.COLETADO, StatusMaterial.COMERCIALIZADO)),
                Sort.by("dataRegistro", "id"));
    }

    private Specification<RegistroMaterial> filtro(Long instituicaoId, LocalDate inicio, LocalDate fim, Usuario usuario, boolean operacional) {
        boolean operadorGlobal = operacional && usuario.getPerfil() == PerfilUsuario.OPERADOR;
        if (!operadorGlobal) escopo.instituicao(instituicaoId, usuario);
        return (r, q, b) -> {
            var periodo = b.between(r.get("dataRegistro"), inicio, fim);
            if (instituicaoId != null) return b.and(periodo, b.equal(r.get("instituicao").get("id"), instituicaoId));
            if (escopo.administrador(usuario) || operadorGlobal) return periodo;
            var instituicao = r.join("instituicao", JoinType.LEFT);
            var titular = b.or(b.equal(r.get("usuario").get("id"), usuario.getId()),
                    b.equal(instituicao.get("usuarioResponsavel").get("id"), usuario.getId()));
            if (operacional && usuario.getPerfil() == PerfilUsuario.COOPERATIVA) {
                var cooperativa = r.join("coleta", JoinType.LEFT).join("cooperativa", JoinType.LEFT);
                titular = b.or(titular, b.and(b.equal(cooperativa.get("usuarioResponsavel").get("id"), usuario.getId()),
                        b.equal(cooperativa.get("status"), StatusInstituicao.ATIVO)));
            }
            return b.and(periodo, titular);
        };
    }

    private void exigirOperador(Coleta coleta, Usuario usuario) {
        if (escopo.administrador(usuario) || usuario.getPerfil() == PerfilUsuario.OPERADOR) return;
        if (usuario.getPerfil() != PerfilUsuario.COOPERATIVA || coleta.getCooperativa() == null || coleta.getCooperativa().getStatus() != StatusInstituicao.ATIVO
                || !coleta.getCooperativa().getUsuarioResponsavel().getId().equals(usuario.getId()))
            throw new AccessDeniedException("Somente a cooperativa responsavel pode registrar o recebimento.");
    }

    private void definirFator(RegistroMaterial material) {
        material.setFatorImpacto(fatores.findFirstByMaterialAndUnidadeAndVigenciaLessThanEqualOrderByVigenciaDescIdDesc(
                material.getTipoMaterial(), material.getUnidade(), material.getDataRegistro()).orElse(null));
    }

    private void registrarEvento(RegistroMaterial material, Usuario autor) {
        eventos.registrar("material:" + material.getId() + ":" + material.getStatus(), "MATERIAL_STATUS", "material",
                material.getId(), autor, material.getUsuario(), material.getInstituicao(), "Material " + material.getStatus() + ".");
    }
}
