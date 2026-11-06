package br.com.overall.partida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubstituicaoRepository extends JpaRepository<Substituicao, Long> {

    List<Substituicao> findByPartidaIdOrderByIdAsc(Long partidaId);

    void deleteByPartidaId(Long partidaId);
}
