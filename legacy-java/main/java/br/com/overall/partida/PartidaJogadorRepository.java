package br.com.overall.partida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartidaJogadorRepository extends JpaRepository<PartidaJogador, Long> {

    List<PartidaJogador> findByPartidaId(Long partidaId);

    Optional<PartidaJogador> findByPartidaIdAndJogadorId(Long partidaId, Long jogadorId);

    boolean existsByJogadorId(Long jogadorId);

    void deleteByPartidaId(Long partidaId);

    /** O jogador já participou de alguma partida desta pelada? */
    @Query("select count(pj) > 0 from PartidaJogador pj, Partida p "
            + "where pj.partidaId = p.id and p.peladaId = :peladaId and pj.jogadorId = :jogadorId")
    boolean participouNaPelada(@Param("peladaId") Long peladaId, @Param("jogadorId") Long jogadorId);
}
