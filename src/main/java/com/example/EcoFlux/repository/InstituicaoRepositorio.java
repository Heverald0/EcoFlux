package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstituicaoRepositorio extends JpaRepository<Instituicao, Long> {

    boolean existsByDocumento(String documento);

    List<Instituicao> findAllByUsuarioResponsavel(Usuario usuarioResponsavel);

    List<Instituicao> findAllByTipoOrderByNomeAsc(TipoInstituicao tipo);

    List<Instituicao> findAllByStatusOrderByDataCriacaoDesc(StatusCadastro status);

    List<Instituicao> findAllByTipoAndStatusOrderByNomeAsc(TipoInstituicao tipo, StatusCadastro status);
}