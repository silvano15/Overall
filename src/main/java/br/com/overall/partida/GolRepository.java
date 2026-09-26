package br.com.overall.partida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface GolRepository extends JpaRepository<Gol, Long> {

    List<Gol> findByPartidaIdOrderByIdAsc(Long partidaId);

    List<Gol> findByPartidaIdIn(Collection<Long> partidaIds);

    long countByPartidaIdAndTimeId(Long partidaId, Long timeId);

    void deleteByPartidaId(Long partidaId);

    /**
     * Duplas garçom → artilheiro nas partidas encerradas do período.
     * Colunas: assistenteId, autorId, quantidade.
     */
    @Query("select g.assistenteId, g.autorId, count(g) "
            + "from Gol g, Partida p, Pelada pe "
            + "where g.partidaId = p.id and p.peladaId = pe.id "
            + "and p.status = br.com.overall.partida.PartidaStatus.ENCERRADA "
            + "and pe.grupoId = :grupoId and pe.dataJogo between :de and :ate and g.assistenteId is not null "
            + "group by g.assistenteId, g.autorId")
    List<Object[]> duplasDeAssistencia(@Param("grupoId") Long grupoId, @Param("de") LocalDate de, @Param("ate") LocalDate ate);
}
