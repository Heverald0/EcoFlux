package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.AmbientalDtos;
import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.*;
import com.example.EcoFlux.exception.*;
import com.example.EcoFlux.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EcoScoreServico {
    private final MovimentoScoreRepositorio movimentos;
    private final CertificadoAmbientalRepositorio certificados;
    private final EscopoAmbiental escopo;
    private final RegraEcoScore regra;
    private final EntityManager entityManager;
    private final EventoServico eventos;

    public void creditar(RegistroMaterial material, Usuario autor) {
        if (!material.getStatus().contabilizado()) throw new ExcecaoConflitoNegocio("Material ainda nao foi recebido.");
        Instituicao instituicao = material.getInstituicao();
        if (instituicao == null) entityManager.find(Usuario.class, material.getUsuario().getId(), LockModeType.PESSIMISTIC_WRITE);
        else entityManager.find(Instituicao.class, instituicao.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (movimentos.existsByMaterialId(material.getId())) return;
        String coluna = instituicao == null ? "usuario.id" : "instituicao.id";
        Long titular = instituicao == null ? material.getUsuario().getId() : instituicao.getId();
        LocalDate data = material.getDataRegistro();
        long hoje = entityManager.createQuery("select count(m) from MovimentoScore m where m." + coluna
                        + " = :id and m.dataReferencia = :data", Long.class)
                .setParameter("id", titular).setParameter("data", data).getSingleResult();
        long anteriores = entityManager.createQuery("select count(m) from MovimentoScore m where m." + coluna
                        + " = :id and m.dataReferencia >= :inicio and m.dataReferencia < :fim", Long.class)
                .setParameter("id", titular).setParameter("inicio", data.minusDays(regra.getJanelaRegularidadeDias()))
                .setParameter("fim", data).getSingleResult();
        int frequencia = hoje == 0 ? regra.getFrequencia() : 0;
        int regularidade = hoje == 0 && anteriores > 0 ? regra.getRegularidade() : 0;
        int quantidade = regra.quantidade(material.getQuantidade());
        int separacao = regra.separacao(material.getTipoMaterial());
        MovimentoScore movimento = MovimentoScore.builder().material(material).instituicao(instituicao)
                .usuario(material.getUsuario()).frequencia(frequencia).regularidade(regularidade)
                .quantidade(quantidade).separacao(separacao).total(frequencia + regularidade + quantidade + separacao)
                .regraVersao(regra.getVersao()).dataReferencia(data).build();
        movimentos.save(movimento);
        eventos.registrar("score:material:" + material.getId(), "ECOSCORE_CREDITO", "material", material.getId(),
                autor, material.getUsuario(), instituicao, "EcoScore: " + movimento.getTotal() + " pontos registrados.");
        if (instituicao != null && instituicao.getStatus() == StatusInstituicao.ATIVO) emitir(instituicao, autor, false);
    }

    @Transactional(readOnly = true)
    public AmbientalDtos.Score consultar(Long instituicaoId, LocalDate inicio, LocalDate fim, Usuario usuario) {
        escopo.periodo(inicio, fim);
        escopo.instituicao(instituicaoId, usuario);
        AmbientalDtos.Pontos pontos = somar(instituicaoId, usuario.getId(), inicio, fim);
        return new AmbientalDtos.Score(instituicaoId, pontos, regra.nivel(pontos.total()), regra.proximo(pontos.total()),
                regra.getVersao(), inicio, fim);
    }

    @Transactional(readOnly = true)
    public Page<AmbientalDtos.Movimento> historico(Long instituicaoId, LocalDate inicio, LocalDate fim,
            int pagina, int tamanho, Usuario usuario) {
        escopo.periodo(inicio, fim);
        escopo.instituicao(instituicaoId, usuario);
        Specification<MovimentoScore> filtro = (r, q, b) -> b.and(
                b.between(r.get("dataReferencia"), inicio, fim),
                instituicaoId == null ? b.equal(r.get("usuario").get("id"), usuario.getId())
                        : b.equal(r.get("instituicao").get("id"), instituicaoId));
        return movimentos.findAll(filtro, PageRequest.of(pagina, Math.min(tamanho, 100),
                Sort.by(Sort.Direction.DESC, "dataCriacao", "id"))).map(AmbientalDtos.Movimento::de);
    }

    @Transactional(readOnly = true)
    public List<AmbientalDtos.Ranking> ranking(LocalDate inicio, LocalDate fim) {
        escopo.periodo(inicio, fim);
        List<Object[]> linhas = entityManager.createQuery("""
                select i.id, i.nome, sum(m.total) from MovimentoScore m join m.instituicao i
                where m.dataReferencia between :inicio and :fim and i.status = :ativo
                group by i.id, i.nome order by sum(m.total) desc, i.id asc
                """, Object[].class).setParameter("inicio", inicio).setParameter("fim", fim)
                .setParameter("ativo", StatusInstituicao.ATIVO).setMaxResults(100).getResultList();
        return linhas.stream().map(l -> new AmbientalDtos.Ranking((Long) l[0], (String) l[1],
                (Long) l[2], regra.nivel((Long) l[2]))).toList();
    }

    public AmbientalDtos.Certificado renovar(Long instituicaoId, Usuario usuario) {
        Instituicao instituicao = escopo.instituicao(instituicaoId, usuario);
        if (instituicao.getStatus() != StatusInstituicao.ATIVO)
            throw new ExcecaoConflitoNegocio("Somente instituicoes ativas podem receber certificados.");
        entityManager.lock(instituicao, LockModeType.PESSIMISTIC_WRITE);
        return AmbientalDtos.Certificado.de(emitir(instituicao, usuario, true));
    }

    @Transactional(readOnly = true)
    public List<AmbientalDtos.Certificado> certificados(Long instituicaoId, Usuario usuario) {
        escopo.instituicao(instituicaoId, usuario);
        return certificados.findByInstituicaoIdOrderByDataCriacaoDescIdDesc(instituicaoId)
                .stream().map(AmbientalDtos.Certificado::de).toList();
    }

    @Transactional(readOnly = true)
    public Page<AmbientalDtos.Certificado> listarCertificados(int pagina, int tamanho, Usuario usuario) {
        Specification<CertificadoAmbiental> filtro = (r, q, b) -> b.and(b.isFalse(r.get("substituido")),
                escopo.administrador(usuario) ? b.conjunction()
                        : b.equal(r.get("instituicao").get("usuarioResponsavel").get("id"), usuario.getId()));
        return certificados.findAll(filtro, PageRequest.of(pagina, Math.min(tamanho, 100),
                Sort.by(Sort.Direction.DESC, "dataCriacao", "id"))).map(AmbientalDtos.Certificado::de);
    }

    private CertificadoAmbiental emitir(Instituicao instituicao, Usuario autor, boolean renovacao) {
        long total = somar(instituicao.getId(), autor.getId(), LocalDate.of(1900, 1, 1), LocalDate.now()).total();
        NivelSelo nivel = regra.nivel(total);
        CertificadoAmbiental anterior = certificados.findFirstByInstituicaoIdOrderByDataCriacaoDescIdDesc(instituicao.getId()).orElse(null);
        if (anterior != null && anterior.getNivel() == nivel && !anterior.getValidade().isBefore(LocalDate.now())) {
            if (renovacao) throw new ExcecaoConflitoNegocio("O certificado atual ainda esta valido e no mesmo nivel.");
            return anterior;
        }
        if (anterior != null) anterior.setSubstituido(true);
        CertificadoAmbiental certificado = CertificadoAmbiental.builder().instituicao(instituicao).nivel(nivel)
                .scoreBase(total).regraVersao(regra.getVersao()).codigo(UUID.randomUUID().toString())
                .emissao(LocalDate.now()).validade(LocalDate.now().plusDays(regra.getValidadeDias()))
                .motivo(anterior == null ? "Emissao inicial" : anterior.getNivel() != nivel ? "Evolucao de nivel" : "Renovacao")
                .autor(autor).build();
        certificados.save(certificado);
        eventos.registrar("selo:" + certificado.getCodigo(), "ECOSELO_EMISSAO", "certificado", certificado.getId(),
                autor, instituicao.getUsuarioResponsavel(), instituicao, "EcoSelo " + nivel + " emitido.");
        return certificado;
    }

    @Transactional(readOnly = true)
    public ResumoSelos resumoSelos(Usuario usuario) {
        String filtro = escopo.administrador(usuario) ? "" : " and c.instituicao.usuarioResponsavel.id = :usuario";
        var consulta = entityManager.createQuery("select c from CertificadoAmbiental c where c.substituido = false" + filtro,
                CertificadoAmbiental.class);
        if (!escopo.administrador(usuario)) consulta.setParameter("usuario", usuario.getId());
        var atuais = consulta.getResultList();
        var validos = atuais.stream().filter(c -> !c.getValidade().isBefore(LocalDate.now()) && c.getNivel() != NivelSelo.INICIAL).toList();
        long ouro = validos.stream().filter(c -> c.getNivel() == NivelSelo.OURO).count();
        long renovacoes = atuais.stream().filter(c -> !c.getValidade().isAfter(LocalDate.now().plusDays(30))).count();
        double aderencia = atuais.stream().mapToDouble(c -> Math.min(100, 100.0 * c.getScoreBase() / regra.getOuro())).average().orElse(0);
        return new ResumoSelos(validos.size(), ouro, java.math.BigDecimal.valueOf(aderencia).setScale(2, java.math.RoundingMode.HALF_UP), renovacoes);
    }

    public record ResumoSelos(long instituicoesCertificadas, long seloOuro, java.math.BigDecimal aderenciaMedia,
                             long renovacoesProximasOuVencidas) {}

    private AmbientalDtos.Pontos somar(Long instituicaoId, Long usuarioId, LocalDate inicio, LocalDate fim) {
        String titular = instituicaoId == null ? "m.usuario.id" : "m.instituicao.id";
        Object[] totais = entityManager.createQuery("select coalesce(sum(m.frequencia),0), coalesce(sum(m.separacao),0), "
                        + "coalesce(sum(m.quantidade),0), coalesce(sum(m.regularidade),0), coalesce(sum(m.total),0) "
                        + "from MovimentoScore m where " + titular + " = :id and m.dataReferencia between :inicio and :fim", Object[].class)
                .setParameter("id", instituicaoId == null ? usuarioId : instituicaoId)
                .setParameter("inicio", inicio).setParameter("fim", fim).getSingleResult();
        return new AmbientalDtos.Pontos(((Number) totais[0]).longValue(), ((Number) totais[1]).longValue(),
                ((Number) totais[2]).longValue(), ((Number) totais[3]).longValue(), ((Number) totais[4]).longValue());
    }
}
