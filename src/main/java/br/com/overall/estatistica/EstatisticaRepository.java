package br.com.overall.estatistica;

import br.com.overall.pelada.Pelada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EstatisticaRepository extends JpaRepository<EstatisticaPartida, Long> {

    List<EstatisticaPartida> findByPeladaId(Long peladaId);

    boolean existsByJogadorId(Long jogadorId);

    void deleteByPeladaId(Long peladaId);

    /** Totais por jogador dentro de um intervalo de datas (rankings mensal/anual/geral). */
    @Query("""
            select e.jogadorId,
                   count(e),
                   sum(case when e.resultado = br.com.overall.estatistica.Resultado.VITORIA then 1 else 0 end),
                   sum(case when e.resultado = br.com.overall.estatistica.Resultado.EMPATE then 1 else 0 end),
                   sum(case when e.resultado = br.com.overall.estatistica.Resultado.DERROTA then 1 else 0 end),
                   sum(e.gols),
                   sum(e.assistencias),
                   sum(case when e.goleiro = true then 1 else 0 end),
                   sum(case when e.semSofrer = true then 1 else 0 end),
                   sum(case when e.goleiro = true then e.golsSofridos else 0 end),
                   avg(e.nota)
            from EstatisticaPartida e, Pelada p
            where e.peladaId = p.id and p.grupoId = :grupoId and e.dataJogo between :de and :ate
            group by e.jogadorId
            """)
    List<Object[]> agregarPorPeriodo(@Param("grupoId") Long grupoId, @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    @Query("select count(distinct e.peladaId) from EstatisticaPartida e, Pelada p "
            + "where e.peladaId = p.id and p.grupoId = :grupoId and e.dataJogo between :de and :ate")
    long contarPeladas(@Param("grupoId") Long grupoId, @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** Totais por jogador de tudo, exceto uma pelada (para calcular o overall "antes"). */
    @Query("""
            select e.jogadorId,
                   count(e),
                   sum(case when e.resultado = br.com.overall.estatistica.Resultado.VITORIA then 1 else 0 end),
                   sum(case when e.resultado = br.com.overall.estatistica.Resultado.EMPATE then 1 else 0 end),
                   sum(case when e.resultado = br.com.overall.estatistica.Resultado.DERROTA then 1 else 0 end),
                   sum(e.gols),
                   sum(e.assistencias),
                   sum(case when e.goleiro = true then 1 else 0 end),
                   sum(case when e.semSofrer = true then 1 else 0 end),
                   sum(case when e.goleiro = true then e.golsSofridos else 0 end),
                   avg(e.nota)
            from EstatisticaPartida e, Pelada p
            where e.peladaId = p.id and p.grupoId = :grupoId and e.peladaId <> :peladaId
            group by e.jogadorId
            """)
    List<Object[]> agregarExceto(@Param("grupoId") Long grupoId, @Param("peladaId") Long peladaId);
}
