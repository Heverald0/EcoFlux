package com.example.EcoFlux.educacao;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.exception.ExcecaoRegraNegocio;
import com.example.EcoFlux.service.EventoServico;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducacaoServico {
    private static final String REGRA_PONTUACAO = "EDUCACAO_SEM_CREDITO_ECOSCORE_V1";
    private final ConteudoEducativoRepositorio conteudos;
    private final ProgressoConteudoRepositorio progressos;
    private final TentativaQuizRepositorio tentativas;
    private final CampanhaEducativaRepositorio campanhas;
    private final ArquivoEducativoRepositorio arquivos;
    private final EntityManager entityManager;
    private final EventoServico eventos;

    @Transactional(readOnly = true)
    public Page<EducacaoRespostas.Conteudo> listar(Usuario usuario, boolean administracao, int pagina, int tamanho) {
        if (administracao) validarAdmin(usuario);
        var paginacao = paginacao(pagina, tamanho, "ordem", "id");
        Map<Long, ProgressoConteudo> porConteudo = progressos.findByUsuarioId(usuario.getId()).stream()
                .collect(Collectors.toMap(p -> p.getConteudo().getId(), Function.identity()));
        var encontrados = administracao ? conteudos.findAll(paginacao) : conteudos.findByPublicadoTrue(paginacao);
        return encontrados.map(c -> resposta(c, Optional.ofNullable(porConteudo.get(c.getId()))
                .map(ProgressoConteudo::getPercentual).orElse(0)));
    }

    @Transactional(readOnly = true)
    public EducacaoRespostas.Conteudo buscar(Long id, Usuario usuario) {
        ConteudoEducativo conteudo = buscarPublicado(id, usuario);
        return resposta(conteudo, progressos.findByUsuarioIdAndConteudoId(usuario.getId(), id)
                .map(ProgressoConteudo::getPercentual).orElse(0));
    }

    @Transactional
    public EducacaoRespostas.Conteudo salvar(Long id, EducacaoRequisicoes.Conteudo pedido, Usuario usuario) {
        validarAdmin(usuario);
        validarUrl(pedido.urlMidia());
        if (pedido.tipo() == TipoConteudo.QUIZ && pedido.perguntas().isEmpty()) {
            throw new ExcecaoRegraNegocio("Um quiz precisa ter pelo menos uma pergunta.");
        }
        if (pedido.tipo() != TipoConteudo.QUIZ && !pedido.perguntas().isEmpty()) {
            throw new ExcecaoRegraNegocio("Perguntas sao permitidas apenas em conteudos do tipo QUIZ.");
        }
        if (pedido.tipo() == TipoConteudo.VIDEO && (pedido.urlMidia() == null || pedido.urlMidia().isBlank())) {
            throw new ExcecaoRegraNegocio("Informe a URL HTTPS do video.");
        }
        ConteudoEducativo conteudo = id == null ? new ConteudoEducativo() : conteudos.buscarParaAtualizar(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conteudo nao encontrado."));
        if (id != null && tentativas.existsByConteudoId(id)) {
            throw new ExcecaoConflitoNegocio("Um quiz respondido deve ser arquivado e substituido por uma nova versao.");
        }
        conteudo.setTitulo(pedido.titulo().trim());
        conteudo.setDescricao(pedido.descricao().trim());
        conteudo.setTipo(pedido.tipo());
        conteudo.setUrlMidia(normalizarUrl(pedido.urlMidia()));
        conteudo.setPublicado(pedido.publicado());
        conteudo.setOrdem(pedido.ordem());
        conteudo.setDestaque(pedido.destaque());
        conteudo.getPerguntas().clear();
        int ordem = 0;
        for (var requisicao : pedido.perguntas()) {
            if (requisicao.alternativaCorreta() >= requisicao.alternativas().size()) {
                throw new ExcecaoRegraNegocio("A alternativa correta deve existir na lista de alternativas.");
            }
            PerguntaQuiz pergunta = new PerguntaQuiz();
            pergunta.setConteudo(conteudo);
            pergunta.setEnunciado(requisicao.enunciado().trim());
            pergunta.setAlternativas(new ArrayList<>(requisicao.alternativas()));
            pergunta.setAlternativaCorreta(requisicao.alternativaCorreta());
            pergunta.setExplicacao(requisicao.explicacao().trim());
            pergunta.setOrdem(ordem++);
            conteudo.getPerguntas().add(pergunta);
        }
        conteudos.saveAndFlush(conteudo);
        auditar(usuario, "CONTEUDO_SALVO", "CONTEUDO", conteudo.getId(), "Conteudo educativo atualizado.");
        return resposta(conteudo, 0);
    }

    @Transactional
    public void arquivar(Long id, Usuario usuario) {
        validarAdmin(usuario);
        ConteudoEducativo conteudo = conteudos.buscarParaAtualizar(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conteudo nao encontrado."));
        conteudo.setPublicado(false);
        auditar(usuario, "CONTEUDO_ARQUIVADO", "CONTEUDO", id, "Conteudo retirado da publicacao.");
    }

    @Transactional
    public EducacaoRespostas.Progresso atualizarProgresso(Long id, EducacaoRequisicoes.Progresso pedido, Usuario usuario) {
        ConteudoEducativo conteudo = bloquearPublicado(id, usuario);
        if (conteudo.getTipo() == TipoConteudo.QUIZ && pedido.percentual() == 100) {
            throw new ExcecaoConflitoNegocio("Conclua o quiz pelo envio das respostas.");
        }
        ProgressoConteudo progresso = obterProgresso(conteudo, usuario);
        atualizarPercentual(progresso, pedido.percentual());
        return respostaProgresso(progresso);
    }

    @Transactional
    public EducacaoRespostas.ResultadoQuiz responder(Long id, EducacaoRequisicoes.Quiz pedido, Usuario usuario) {
        ConteudoEducativo conteudo = bloquearPublicado(id, usuario);
        if (conteudo.getTipo() != TipoConteudo.QUIZ) {
            throw new ExcecaoConflitoNegocio("O conteudo nao e um quiz.");
        }
        ProgressoConteudo progresso = obterProgresso(conteudo, usuario);
        if (progresso.getPercentual() == 100) {
            throw new ExcecaoConflitoNegocio("Este quiz ja foi concluido.");
        }
        if (tentativas.countByUsuarioIdAndConteudoId(usuario.getId(), id) >= 10) {
            throw new ExcecaoConflitoNegocio("Limite de dez tentativas atingido para este quiz.");
        }
        Map<Long, Integer> respostas = new HashMap<>();
        for (var resposta : pedido.respostas()) {
            if (respostas.put(resposta.perguntaId(), resposta.alternativa()) != null) {
                throw new ExcecaoRegraNegocio("Cada pergunta deve ser respondida uma unica vez.");
            }
        }
        Set<Long> ids = conteudo.getPerguntas().stream().map(PerguntaQuiz::getId).collect(Collectors.toSet());
        if (!ids.equals(respostas.keySet())) {
            throw new ExcecaoRegraNegocio("Responda todas as perguntas deste quiz.");
        }
        List<EducacaoRespostas.Correcao> correcoes = new ArrayList<>();
        int acertos = 0;
        for (PerguntaQuiz pergunta : conteudo.getPerguntas()) {
            int alternativa = respostas.get(pergunta.getId());
            if (alternativa >= pergunta.getAlternativas().size()) {
                throw new ExcecaoRegraNegocio("Alternativa inexistente.");
            }
            boolean correta = alternativa == pergunta.getAlternativaCorreta();
            if (correta) acertos++;
            correcoes.add(new EducacaoRespostas.Correcao(pergunta.getId(), correta,
                    pergunta.getAlternativaCorreta(), pergunta.getExplicacao()));
        }
        TentativaQuiz tentativa = new TentativaQuiz();
        tentativa.setUsuario(progresso.getUsuario());
        tentativa.setConteudo(conteudo);
        tentativa.setAcertos(acertos);
        tentativa.setTotal(conteudo.getPerguntas().size());
        tentativa.setAprovado(acertos == tentativa.getTotal());
        tentativa.setRespondidoEm(LocalDateTime.now());
        tentativas.save(tentativa);
        atualizarPercentual(progresso, tentativa.isAprovado() ? 100 : 50);
        auditar(usuario, "QUIZ_RESPONDIDO", "TENTATIVA_QUIZ", tentativa.getId(),
                "Quiz respondido: " + acertos + "/" + tentativa.getTotal() + " acertos.");
        return new EducacaoRespostas.ResultadoQuiz(tentativa.getId(), acertos, tentativa.getTotal(),
                tentativa.isAprovado(), 0, REGRA_PONTUACAO, correcoes);
    }

    @Transactional(readOnly = true)
    public EducacaoRespostas.Trilha trilha(Usuario usuario) {
        Map<Long, Integer> percentuais = progressos.findByUsuarioId(usuario.getId()).stream()
                .collect(Collectors.toMap(p -> p.getConteudo().getId(), ProgressoConteudo::getPercentual));
        var etapas = conteudos.findByPublicadoTrueOrderByOrdemAscIdAsc().stream()
                .map(c -> new EducacaoRespostas.Etapa(c.getId(), c.getTitulo(), c.getTipo(), c.getOrdem(),
                        percentuais.getOrDefault(c.getId(), 0))).toList();
        return new EducacaoRespostas.Trilha((int) etapas.stream().mapToInt(EducacaoRespostas.Etapa::progresso)
                .average().orElse(0), etapas);
    }

    @Transactional(readOnly = true)
    public EducacaoRespostas.Indicadores indicadores() {
        return new EducacaoRespostas.Indicadores(conteudos.countByPublicadoTrue(), progressos.contarUsuariosEngajados(),
                tentativas.count(), 0, REGRA_PONTUACAO);
    }

    @Transactional
    public EducacaoRespostas.Campanha salvarCampanha(Long id, EducacaoRequisicoes.Campanha pedido, Usuario usuario) {
        validarAdmin(usuario);
        validarUrl(pedido.urlAcao());
        if (pedido.fim().isBefore(pedido.inicio())) {
            throw new ExcecaoRegraNegocio("O fim da campanha deve ser posterior ou igual ao inicio.");
        }
        CampanhaEducativa campanha = id == null ? new CampanhaEducativa() : campanhas.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Campanha nao encontrada."));
        campanha.setTitulo(pedido.titulo().trim());
        campanha.setDescricao(pedido.descricao().trim());
        campanha.setInicio(pedido.inicio());
        campanha.setFim(pedido.fim());
        campanha.setUrlAcao(normalizarUrl(pedido.urlAcao()));
        campanha.setPublicada(pedido.publicada());
        campanhas.save(campanha);
        auditar(usuario, "CAMPANHA_SALVA", "CAMPANHA", campanha.getId(), "Campanha educativa atualizada.");
        return respostaCampanha(campanha);
    }

    @Transactional
    public void arquivarCampanha(Long id, Usuario usuario) {
        validarAdmin(usuario);
        CampanhaEducativa campanha = campanhas.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Campanha nao encontrada."));
        campanha.setPublicada(false);
        auditar(usuario, "CAMPANHA_ARQUIVADA", "CAMPANHA", id, "Campanha retirada da publicacao.");
    }

    @Transactional(readOnly = true)
    public Page<EducacaoRespostas.Campanha> listarCampanhas(Usuario usuario, boolean administracao, int pagina, int tamanho) {
        if (administracao) validarAdmin(usuario);
        var paginacao = paginacao(pagina, tamanho, "inicio", "id");
        var resultado = administracao ? campanhas.findAll(paginacao)
                : campanhas.findByPublicadaTrueAndInicioLessThanEqualAndFimGreaterThanEqual(LocalDate.now(), LocalDate.now(), paginacao);
        return resultado.map(this::respostaCampanha);
    }

    @Transactional
    public void salvarArquivo(Long id, MultipartFile arquivo, Usuario usuario) {
        validarAdmin(usuario);
        ConteudoEducativo conteudo = conteudos.buscarParaAtualizar(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conteudo nao encontrado."));
        if (conteudo.getTipo() != TipoConteudo.GUIA || arquivo.isEmpty() || arquivo.getSize() > 5 * 1024 * 1024) {
            throw new ExcecaoRegraNegocio("Envie um arquivo PDF de ate 5 MB para um conteudo do tipo GUIA.");
        }
        try {
            byte[] dados = arquivo.getBytes();
            if (dados.length < 5 || !new String(dados, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
                throw new ExcecaoRegraNegocio("O arquivo deve ser um PDF valido.");
            }
            try (var documento = Loader.loadPDF(dados)) {
                if (documento.isEncrypted() || documento.getNumberOfPages() == 0) {
                    throw new ExcecaoRegraNegocio("Envie um PDF sem senha e com pelo menos uma pagina.");
                }
            }
            ArquivoEducativo registro = arquivos.findById(id).orElseGet(ArquivoEducativo::new);
            registro.setConteudo(conteudo);
            registro.setDados(dados);
            arquivos.save(registro);
            auditar(usuario, "GUIA_ATUALIZADO", "CONTEUDO", id, "Arquivo PDF do guia atualizado.");
        } catch (IOException excecao) {
            throw new ExcecaoRegraNegocio("Nao foi possivel abrir o PDF enviado. Verifique o arquivo e remova eventuais senhas.");
        }
    }

    @Transactional
    public byte[] baixarArquivo(Long id, Usuario usuario) {
        buscarPublicado(id, usuario);
        ArquivoEducativo arquivo = arquivos.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Arquivo nao disponivel para este conteudo."));
        auditar(usuario, "GUIA_BAIXADO", "CONTEUDO", id, "Download de guia educativo.");
        return arquivo.getDados();
    }

    @Transactional(readOnly = true)
    public EducacaoRespostas.Download download(Long id, Usuario usuario) {
        ConteudoEducativo conteudo = buscarPublicado(id, usuario);
        if (!arquivos.existsById(id)) throw new ExcecaoRecursoNaoEncontrado("Arquivo nao disponivel para este conteudo.");
        return new EducacaoRespostas.Download(id, conteudo.getTitulo(), "/api/v1/educacao/conteudos/" + id + "/arquivo", "application/pdf");
    }

    private ConteudoEducativo bloquearPublicado(Long id, Usuario usuario) {
        ConteudoEducativo conteudo = conteudos.buscarParaAtualizar(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conteudo nao encontrado."));
        if (!conteudo.isPublicado()) throw new ExcecaoRecursoNaoEncontrado("Conteudo nao publicado.");
        entityManager.find(Usuario.class, usuario.getId(), LockModeType.PESSIMISTIC_WRITE);
        return conteudo;
    }

    private ProgressoConteudo obterProgresso(ConteudoEducativo conteudo, Usuario usuario) {
        return progressos.findByUsuarioIdAndConteudoId(usuario.getId(), conteudo.getId()).orElseGet(() -> {
            ProgressoConteudo novo = new ProgressoConteudo();
            novo.setUsuario(entityManager.getReference(Usuario.class, usuario.getId()));
            novo.setConteudo(conteudo);
            novo.setIniciadoEm(LocalDateTime.now());
            return progressos.save(novo);
        });
    }

    private void atualizarPercentual(ProgressoConteudo progresso, int percentual) {
        if (percentual > progresso.getPercentual()) progresso.setPercentual(percentual);
        if (progresso.getPercentual() == 100 && progresso.getConcluidoEm() == null) progresso.setConcluidoEm(LocalDateTime.now());
    }

    private ConteudoEducativo buscarPublicado(Long id, Usuario usuario) {
        ConteudoEducativo conteudo = conteudos.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conteudo nao encontrado."));
        if (!conteudo.isPublicado() && usuario.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
            throw new ExcecaoRecursoNaoEncontrado("Conteudo nao encontrado.");
        }
        return conteudo;
    }

    private EducacaoRespostas.Conteudo resposta(ConteudoEducativo c, int percentual) {
        var perguntas = c.getPerguntas().stream().map(p -> new EducacaoRespostas.Pergunta(p.getId(),
                p.getEnunciado(), List.copyOf(p.getAlternativas()))).toList();
        return new EducacaoRespostas.Conteudo(c.getId(), c.getTitulo(), c.getDescricao(), c.getTipo(),
                c.getUrlMidia(), c.isPublicado(), c.getOrdem(), c.isDestaque(), percentual, perguntas);
    }

    private EducacaoRespostas.Progresso respostaProgresso(ProgressoConteudo p) {
        return new EducacaoRespostas.Progresso(p.getConteudo().getId(), p.getPercentual(), p.getIniciadoEm(), p.getConcluidoEm());
    }

    private EducacaoRespostas.Campanha respostaCampanha(CampanhaEducativa c) {
        String status = !c.isPublicada() ? "ARQUIVADA" : c.getFim().isBefore(LocalDate.now()) ? "ENCERRADA"
                : c.getInicio().isAfter(LocalDate.now()) ? "PROGRAMADA" : "ATIVA";
        return new EducacaoRespostas.Campanha(c.getId(), c.getTitulo(), c.getDescricao(), c.getInicio(), c.getFim(),
                c.getUrlAcao(), c.isPublicada(), status);
    }

    private PageRequest paginacao(int pagina, int tamanho, String... ordem) {
        return PageRequest.of(Math.max(0, pagina), Math.max(1, Math.min(100, tamanho)), Sort.by(ordem));
    }

    private void validarAdmin(Usuario usuario) {
        if (usuario.getPerfil() != PerfilUsuario.ADMINISTRADOR) throw new AccessDeniedException("Acao exclusiva de administrador.");
    }

    private String normalizarUrl(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validarUrl(String valor) {
        if (valor == null || valor.isBlank()) return;
        try {
            URI uri = URI.create(valor.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException excecao) {
            throw new ExcecaoRegraNegocio("Informe uma URL HTTPS valida e sem credenciais.");
        }
    }

    private void auditar(Usuario usuario, String tipo, String entidade, Long id, String mensagem) {
        eventos.registrar(UUID.randomUUID().toString(), tipo, entidade, id, usuario, null, null, mensagem);
    }
}
