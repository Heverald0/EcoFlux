package com.example.EcoFlux.educacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class EducacaoRespostas {
    private EducacaoRespostas() {}

    public record Pergunta(Long id, String enunciado, List<String> alternativas) {}
    public record Conteudo(Long id, String titulo, String descricao, TipoConteudo tipo, String urlMidia,
                           boolean publicado, int ordem, boolean destaque, int progresso,
                           List<Pergunta> perguntas) {}
    public record Progresso(Long conteudoId, int percentual, LocalDateTime iniciadoEm, LocalDateTime concluidoEm) {}
    public record Correcao(Long perguntaId, boolean correta, int alternativaCorreta, String explicacao) {}
    public record ResultadoQuiz(Long tentativaId, int acertos, int total, boolean aprovado, int pontosGerados,
                                String regraPontuacao, List<Correcao> correcoes) {}
    public record Campanha(Long id, String titulo, String descricao, LocalDate inicio, LocalDate fim,
                           String urlAcao, boolean publicada, String status) {}
    public record Indicadores(long conteudosPublicados, long usuariosEngajados, long quizzesRespondidos,
                              long pontosGerados, String regraPontuacao) {}
    public record Etapa(Long conteudoId, String titulo, TipoConteudo tipo, int ordem, int progresso) {}
    public record Trilha(int progressoPercentual, List<Etapa> etapas) {}
    public record Download(Long conteudoId, String titulo, String url, String tipo) {}
}
