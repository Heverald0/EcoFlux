package com.example.EcoFlux.educacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class EducacaoRequisicoes {
    private EducacaoRequisicoes() {}

    public record Conteudo(@NotBlank @Size(max = 200) String titulo,
                           @NotBlank @Size(max = 5000) String descricao,
                           @NotNull TipoConteudo tipo, @Size(max = 1500) String urlMidia,
                           boolean publicado, @Min(0) int ordem, boolean destaque,
                           @NotNull @Size(max = 30) List<@Valid Pergunta> perguntas) {}

    public record Pergunta(@NotBlank @Size(max = 1000) String enunciado,
                           @NotEmpty @Size(min = 2, max = 8) List<@NotBlank @Size(max = 1000) String> alternativas,
                           @Min(0) int alternativaCorreta, @NotBlank @Size(max = 2000) String explicacao) {}

    public record Progresso(@Min(0) @Max(100) int percentual) {}

    public record Resposta(@NotNull @Positive Long perguntaId, @Min(0) int alternativa) {}

    public record Quiz(@NotEmpty @Size(max = 30) List<@Valid Resposta> respostas) {}

    public record Campanha(@NotBlank @Size(max = 200) String titulo,
                           @NotBlank @Size(max = 2000) String descricao,
                           @NotNull LocalDate inicio, @NotNull LocalDate fim,
                           @Size(max = 1500) String urlAcao, boolean publicada) {}
}
