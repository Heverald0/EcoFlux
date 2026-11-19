package com.example.EcoFlux.rotas;

import com.example.EcoFlux.dto.request.AtualizarStatusColetaRequisicao;
import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusColeta;
import com.example.EcoFlux.entity.enums.StatusPontoColeta;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.repository.ColetaRepositorio;
import com.example.EcoFlux.repository.InstituicaoRepositorio;
import com.example.EcoFlux.service.ColetaServico;
import com.example.EcoFlux.service.EventoServico;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RotaServico {
    private final RotaRepositorio rotas;
    private final ParadaRotaRepositorio paradas;
    private final ColetaRepositorio coletas;
    private final InstituicaoRepositorio instituicoes;
    private final ColetaServico coletaServico;
    private final EventoServico eventos;

    @Value("${ecoflux.rotas.velocidade-media-kmh:30}")
    private double velocidadeMediaKmh;
    @Value("${ecoflux.rotas.minutos-por-parada:10}")
    private int minutosPorParada;

    @Transactional
    public RotaResposta planejar(RotaRequisicoes.Planejar pedido, Usuario usuario) {
        Instituicao cooperativa = instituicoes.findById(pedido.cooperativaId())
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Cooperativa nao encontrada."));
        validarCooperativa(cooperativa, usuario);
        if (new HashSet<>(pedido.coletaIds()).size() != pedido.coletaIds().size()) {
            throw new ExcecaoConflitoNegocio("Uma coleta nao pode aparecer duas vezes na rota.");
        }
        Map<Long, Coleta> selecionadas = new HashMap<>();
        pedido.coletaIds().stream().sorted().forEach(id -> {
            Coleta coleta = coletas.buscarParaAtualizar(id)
                    .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Coleta nao encontrada: " + id));
            validarElegibilidade(coleta, cooperativa, pedido.partida());
            if (paradas.existsByColetaId(id)) {
                throw new ExcecaoConflitoNegocio("A coleta " + id + " ja pertence a uma rota.");
            }
            selecionadas.put(id, coleta);
        });
        Rota rota = new Rota();
        rota.setNome(pedido.nome().trim());
        rota.setCooperativa(cooperativa);
        rota.setAutor(usuario);
        rota.setPartida(pedido.partida());
        rota.setLatitudeOrigem(pedido.latitudeOrigem());
        rota.setLongitudeOrigem(pedido.longitudeOrigem());
        for (Long id : pedido.coletaIds()) {
            Coleta coleta = selecionadas.get(id);
            ParadaRota parada = new ParadaRota();
            parada.setRota(rota);
            parada.setColeta(coleta);
            parada.setPonto(coleta.getPontoColeta());
            parada.setLatitude(coleta.getPontoColeta().getLatitude());
            parada.setLongitude(coleta.getPontoColeta().getLongitude());
            rota.getParadas().add(parada);
        }
        recalcular(rota);
        rotas.saveAndFlush(rota);
        registrar(rota, usuario, "ROTA_PLANEJADA", "Rota planejada com " + rota.getParadas().size() + " paradas.");
        return RotaResposta.de(rota);
    }

    @Transactional(readOnly = true)
    public Page<RotaResposta> listar(Usuario usuario, StatusRota status, int pagina, int tamanho) {
        return rotas.listar(usuario.getId(), acessoGlobal(usuario), status,
                PageRequest.of(Math.max(0, pagina), Math.min(100, Math.max(1, tamanho)),
                        Sort.by(Sort.Direction.DESC, "partida", "id"))).map(RotaResposta::de);
    }

    @Transactional(readOnly = true)
    public RotaResposta buscar(Long id, Usuario usuario) {
        Rota rota = rotas.findById(id).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Rota nao encontrada."));
        validarAcesso(rota, usuario);
        return RotaResposta.de(rota);
    }

    @Transactional
    public RotaResposta otimizar(Long id, Usuario usuario) {
        Rota rota = carregar(id, usuario);
        validarPlanejada(rota);
        List<ParadaRota> restantes = new ArrayList<>(rota.getParadas());
        List<ParadaRota> ordenadas = new ArrayList<>();
        double latitude = rota.getLatitudeOrigem().doubleValue();
        double longitude = rota.getLongitudeOrigem().doubleValue();
        while (!restantes.isEmpty()) {
            double origemLatitude = latitude;
            double origemLongitude = longitude;
            ParadaRota proxima = restantes.stream().min(Comparator
                    .comparingDouble((ParadaRota p) -> DistanciaGeografica.km(origemLatitude, origemLongitude,
                            p.getLatitude().doubleValue(), p.getLongitude().doubleValue()))
                    .thenComparing(p -> p.getColeta().getId())).orElseThrow();
            restantes.remove(proxima);
            ordenadas.add(proxima);
            latitude = proxima.getLatitude().doubleValue();
            longitude = proxima.getLongitude().doubleValue();
        }
        rota.getParadas().clear();
        rota.getParadas().addAll(ordenadas);
        recalcular(rota);
        rota.setStatus(StatusRota.OTIMIZADA);
        registrar(rota, usuario, "ROTA_OTIMIZADA", "Ordem otimizada por proximidade geografica.");
        return RotaResposta.de(rota);
    }

    @Transactional
    public RotaResposta iniciar(Long id, Usuario usuario) {
        Rota rota = carregar(id, usuario);
        validarPlanejada(rota);
        validarCooperativa(rota.getCooperativa(), usuario);
        for (ParadaRota parada : rota.getParadas()) {
            validarElegibilidade(parada.getColeta(), rota.getCooperativa(), rota.getPartida());
        }
        rota.setStatus(StatusRota.EM_ANDAMENTO);
        rota.setIniciadaEm(LocalDateTime.now());
        avancar(rota, usuario);
        registrar(rota, usuario, "ROTA_INICIADA", "Execucao da rota iniciada.");
        return RotaResposta.de(rota);
    }

    @Transactional
    public RotaResposta concluirParada(Long rotaId, Long paradaId, RotaRequisicoes.Concluir pedido, Usuario usuario) {
        Rota rota = carregar(rotaId, usuario);
        ParadaRota parada = paradaAtual(rota, paradaId);
        coletaServico.atualizarStatus(parada.getColeta().getId(),
                new AtualizarStatusColetaRequisicao(StatusColeta.REALIZADA, pedido.observacao(), null,
                        pedido.quantidadeReal()), usuario);
        parada.setStatus(StatusParada.CONCLUIDA);
        parada.setConcluidaEm(LocalDateTime.now());
        avancar(rota, usuario);
        return RotaResposta.de(rota);
    }

    @Transactional
    public RotaResposta falharParada(Long rotaId, Long paradaId, RotaRequisicoes.Falhar pedido, Usuario usuario) {
        Rota rota = carregar(rotaId, usuario);
        ParadaRota parada = paradaAtual(rota, paradaId);
        coletaServico.atualizarStatus(parada.getColeta().getId(),
                new AtualizarStatusColetaRequisicao(StatusColeta.CANCELADA, pedido.motivo().trim(), null), usuario);
        parada.setStatus(StatusParada.NAO_ATENDIDA);
        parada.setMotivo(pedido.motivo().trim());
        parada.setConcluidaEm(LocalDateTime.now());
        avancar(rota, usuario);
        return RotaResposta.de(rota);
    }

    private void avancar(Rota rota, Usuario usuario) {
        Optional<ParadaRota> proxima = rota.getParadas().stream().filter(p -> p.getStatus() == StatusParada.PENDENTE)
                .min(Comparator.comparingInt(ParadaRota::getOrdem));
        if (proxima.isPresent()) {
            ParadaRota parada = proxima.get();
            coletaServico.atualizarStatus(parada.getColeta().getId(),
                    new AtualizarStatusColetaRequisicao(StatusColeta.EM_ROTA, "Inicio da parada da rota " + rota.getId(), null), usuario);
            parada.setStatus(StatusParada.EM_ROTA);
        } else {
            rota.setStatus(StatusRota.CONCLUIDA);
            rota.setConcluidaEm(LocalDateTime.now());
            registrar(rota, usuario, "ROTA_CONCLUIDA", "Todas as paradas foram concluidas ou justificadas.");
        }
    }

    private ParadaRota paradaAtual(Rota rota, Long paradaId) {
        if (rota.getStatus() != StatusRota.EM_ANDAMENTO) {
            throw new ExcecaoConflitoNegocio("A rota nao esta em andamento.");
        }
        ParadaRota parada = rota.getParadas().stream().filter(p -> p.getId().equals(paradaId)).findFirst()
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Parada nao encontrada nesta rota."));
        if (parada.getStatus() != StatusParada.EM_ROTA) {
            throw new ExcecaoConflitoNegocio("Somente a parada atual pode ser atualizada.");
        }
        return parada;
    }

    private void recalcular(Rota rota) {
        if (velocidadeMediaKmh <= 0 || minutosPorParada < 0) {
            throw new IllegalStateException("Configuracao de estimativa de rota invalida.");
        }
        double latitude = rota.getLatitudeOrigem().doubleValue();
        double longitude = rota.getLongitudeOrigem().doubleValue();
        double distancia = 0;
        int minutos = 0;
        int ordem = 1;
        for (ParadaRota parada : rota.getParadas()) {
            double trecho = DistanciaGeografica.km(latitude, longitude,
                    parada.getLatitude().doubleValue(), parada.getLongitude().doubleValue());
            distancia += trecho;
            minutos += (int) Math.ceil(trecho / velocidadeMediaKmh * 60);
            parada.setOrdem(ordem++);
            parada.setDistanciaTrechoKm(BigDecimal.valueOf(trecho).setScale(3, RoundingMode.HALF_UP));
            parada.setPrevisao(rota.getPartida().plusMinutes(minutos));
            minutos += minutosPorParada;
            latitude = parada.getLatitude().doubleValue();
            longitude = parada.getLongitude().doubleValue();
        }
        rota.setDistanciaKm(BigDecimal.valueOf(distancia).setScale(3, RoundingMode.HALF_UP));
        rota.setDuracaoMinutos(minutos);
    }

    private void validarElegibilidade(Coleta coleta, Instituicao cooperativa, LocalDateTime partida) {
        if (coleta.getStatus() != StatusColeta.AGENDADA || coleta.getCooperativa() == null
                || !coleta.getCooperativa().getId().equals(cooperativa.getId())
                || !coleta.getDataHoraAgendamento().toLocalDate().equals(partida.toLocalDate())) {
            throw new ExcecaoConflitoNegocio("A rota exige coletas agendadas na mesma data e cooperativa.");
        }
        var ponto = coleta.getPontoColeta();
        if (ponto == null || ponto.getStatus() != StatusPontoColeta.ATIVO
                || ponto.getLatitude() == null || ponto.getLongitude() == null
                || !ponto.getMateriaisAceitos().contains(coleta.getTipoMaterial())) {
            throw new ExcecaoConflitoNegocio("A coleta exige ponto ativo, coordenadas e material aceito.");
        }
    }

    private Rota carregar(Long id, Usuario usuario) {
        Rota rota = rotas.buscarParaAtualizar(id).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Rota nao encontrada."));
        validarAcesso(rota, usuario);
        validarCooperativa(rota.getCooperativa(), usuario);
        return rota;
    }

    private void validarPlanejada(Rota rota) {
        if (rota.getStatus() != StatusRota.PLANEJADA && rota.getStatus() != StatusRota.OTIMIZADA) {
            throw new ExcecaoConflitoNegocio("A rota ja foi iniciada.");
        }
    }

    private void validarCooperativa(Instituicao cooperativa, Usuario usuario) {
        if (cooperativa.getTipo() != TipoInstituicao.COOPERATIVA || !"ATIVO".equals(cooperativa.getStatus().name())) {
            throw new ExcecaoConflitoNegocio("Informe uma cooperativa ativa.");
        }
        if (!acessoGlobal(usuario) && (usuario.getPerfil() != PerfilUsuario.COOPERATIVA
                || !cooperativa.getUsuarioResponsavel().getId().equals(usuario.getId()))) {
            throw new AccessDeniedException("A rota pertence a outra cooperativa.");
        }
    }

    private void validarAcesso(Rota rota, Usuario usuario) {
        if (!acessoGlobal(usuario) && !rota.getCooperativa().getUsuarioResponsavel().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("A rota pertence a outra cooperativa.");
        }
    }

    private boolean acessoGlobal(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR || usuario.getPerfil() == PerfilUsuario.OPERADOR;
    }

    private void registrar(Rota rota, Usuario usuario, String tipo, String mensagem) {
        eventos.registrar(UUID.randomUUID().toString(), tipo, "ROTA", rota.getId(), usuario,
                rota.getCooperativa().getUsuarioResponsavel(), rota.getCooperativa(), mensagem);
    }
}
