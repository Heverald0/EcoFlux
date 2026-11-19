package com.example.EcoFlux.service;

import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.repository.EventoRepositorio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class EventoServico {
    private final EventoRepositorio repositorio;

    @Transactional
    public Evento registrar(String chave, String tipo, String entidade, Long entidadeId, Usuario autor,
                            Usuario destinatario, Instituicao instituicao, String mensagem) {
        return repositorio.findByChave(chave).orElseGet(() -> repositorio.save(Evento.builder()
                .chave(chave).tipo(tipo).entidade(entidade).entidadeId(entidadeId).autor(autor)
                .destinatario(destinatario).instituicao(instituicao).mensagem(mensagem).build()));
    }

    @Transactional(readOnly = true)
    public Page<Notificacao> listar(Usuario usuario, boolean naoLidas, Pageable pageable) {
        return (naoLidas ? repositorio.findByDestinatarioIdAndDataLeituraIsNullOrderByDataCriacaoDesc(usuario.getId(), pageable)
                : repositorio.findByDestinatarioIdOrderByDataCriacaoDesc(usuario.getId(), pageable)).map(Notificacao::de);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Usuario usuario) {
        return repositorio.countByDestinatarioIdAndDataLeituraIsNull(usuario.getId());
    }

    @Transactional
    public Notificacao ler(Long id, Usuario usuario) {
        Evento evento = repositorio.findByIdAndDestinatarioId(id, usuario.getId())
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Notificacao nao encontrada."));
        if (evento.getDataLeitura() == null) evento.setDataLeitura(LocalDateTime.now());
        return Notificacao.de(evento);
    }

    public record Notificacao(Long id, String tipo, String entidade, Long entidadeId, String mensagem,
                              LocalDateTime dataCriacao, LocalDateTime dataLeitura) {
        public static Notificacao de(Evento evento) {
            return new Notificacao(evento.getId(), evento.getTipo(), evento.getEntidade(), evento.getEntidadeId(),
                    evento.getMensagem(), evento.getDataCriacao(), evento.getDataLeitura());
        }
    }
}
