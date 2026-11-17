package com.example.EcoFlux.educacao;

import com.example.EcoFlux.entity.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/educacao")
@RequiredArgsConstructor
public class EducacaoControlador {
    private final EducacaoServico servico;

    @GetMapping("/conteudos")
    public Page<EducacaoRespostas.Conteudo> listar(@AuthenticationPrincipal Usuario usuario,
                                                 @RequestParam(defaultValue = "0") int pagina,
                                                 @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listar(usuario, false, pagina, tamanho);
    }

    @GetMapping("/conteudos/{id}")
    public EducacaoRespostas.Conteudo buscar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return servico.buscar(id, usuario);
    }

    @GetMapping("/admin/conteudos")
    public Page<EducacaoRespostas.Conteudo> listarAdministracao(@AuthenticationPrincipal Usuario usuario,
                                                              @RequestParam(defaultValue = "0") int pagina,
                                                              @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listar(usuario, true, pagina, tamanho);
    }

    @PostMapping("/admin/conteudos") @ResponseStatus(HttpStatus.CREATED)
    public EducacaoRespostas.Conteudo criar(@RequestBody @Valid EducacaoRequisicoes.Conteudo pedido,
                                            @AuthenticationPrincipal Usuario usuario) {
        return servico.salvar(null, pedido, usuario);
    }

    @PutMapping("/admin/conteudos/{id}")
    public EducacaoRespostas.Conteudo atualizar(@PathVariable Long id,
                                               @RequestBody @Valid EducacaoRequisicoes.Conteudo pedido,
                                               @AuthenticationPrincipal Usuario usuario) {
        return servico.salvar(id, pedido, usuario);
    }

    @DeleteMapping("/admin/conteudos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void arquivar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        servico.arquivar(id, usuario);
    }

    @PutMapping("/conteudos/{id}/progresso")
    public EducacaoRespostas.Progresso progresso(@PathVariable Long id,
                                                 @RequestBody @Valid EducacaoRequisicoes.Progresso pedido,
                                                 @AuthenticationPrincipal Usuario usuario) {
        return servico.atualizarProgresso(id, pedido, usuario);
    }

    @PostMapping("/conteudos/{id}/respostas")
    public EducacaoRespostas.ResultadoQuiz responder(@PathVariable Long id,
                                                    @RequestBody @Valid EducacaoRequisicoes.Quiz pedido,
                                                    @AuthenticationPrincipal Usuario usuario) {
        return servico.responder(id, pedido, usuario);
    }

    @GetMapping("/trilha")
    public EducacaoRespostas.Trilha trilha(@AuthenticationPrincipal Usuario usuario) {
        return servico.trilha(usuario);
    }

    @GetMapping("/indicadores")
    public EducacaoRespostas.Indicadores indicadores() {
        return servico.indicadores();
    }

    @GetMapping("/campanhas")
    public Page<EducacaoRespostas.Campanha> campanhas(@AuthenticationPrincipal Usuario usuario,
                                                     @RequestParam(defaultValue = "0") int pagina,
                                                     @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listarCampanhas(usuario, false, pagina, tamanho);
    }

    @GetMapping("/admin/campanhas")
    public Page<EducacaoRespostas.Campanha> campanhasAdministracao(@AuthenticationPrincipal Usuario usuario,
                                                                  @RequestParam(defaultValue = "0") int pagina,
                                                                  @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listarCampanhas(usuario, true, pagina, tamanho);
    }

    @PostMapping("/admin/campanhas") @ResponseStatus(HttpStatus.CREATED)
    public EducacaoRespostas.Campanha criarCampanha(@RequestBody @Valid EducacaoRequisicoes.Campanha pedido,
                                                   @AuthenticationPrincipal Usuario usuario) {
        return servico.salvarCampanha(null, pedido, usuario);
    }

    @PutMapping("/admin/campanhas/{id}")
    public EducacaoRespostas.Campanha atualizarCampanha(@PathVariable Long id,
                                                       @RequestBody @Valid EducacaoRequisicoes.Campanha pedido,
                                                       @AuthenticationPrincipal Usuario usuario) {
        return servico.salvarCampanha(id, pedido, usuario);
    }

    @DeleteMapping("/admin/campanhas/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void arquivarCampanha(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        servico.arquivarCampanha(id, usuario);
    }

    @PutMapping(value = "/admin/conteudos/{id}/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void arquivo(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo,
                        @AuthenticationPrincipal Usuario usuario) {
        servico.salvarArquivo(id, arquivo, usuario);
    }

    @GetMapping("/conteudos/{id}/download")
    public EducacaoRespostas.Download download(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return servico.download(id, usuario);
    }

    @GetMapping(value = "/conteudos/{id}/arquivo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> arquivo(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guia-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(servico.baixarArquivo(id, usuario));
    }
}
