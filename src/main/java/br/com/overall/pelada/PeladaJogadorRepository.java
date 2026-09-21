package br.com.overall.pelada;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PeladaJogadorRepository extends JpaRepository<PeladaJogador, Long> {

    List<PeladaJogador> findByPeladaId(Long peladaId);

    Optional<PeladaJogador> findByPeladaIdAndJogadorId(Long peladaId, Long jogadorId);

    boolean existsByPeladaIdAndJogadorId(Long peladaId, Long jogadorId);

    boolean existsByJogadorId(Long jogadorId);

    void deleteByPeladaId(Long peladaId);

    List<PeladaJogador> findByJogadorId(Long jogadorId);

    /**
     * Overall antes/depois de cada jogador em cada pelada finalizada do período, em ordem cronológica.
     * Colunas: jogadorId, overallAntes, overallDepois.
     */
    @Query("select pj.jogadorId, pj.overallAntes, pj.overallDepois "
            + "from PeladaJogador pj, Pelada p "
            + "where pj.peladaId = p.id and p.status = br.com.overall.pelada.PeladaStatus.FINALIZADA "
            + "and p.dataJogo between :de and :ate and pj.overallDepois is not null "
            + "order by p.dataJogo asc, p.id asc")
    List<Object[]> evolucaoDoOverall(@Param("de") LocalDate de, @Param("ate") LocalDate ate);
}
