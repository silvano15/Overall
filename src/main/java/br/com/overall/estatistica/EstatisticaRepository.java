package br.com.overall.estatistica;

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
            from EstatisticaPartida e
            where e.dataJogo between :de and :ate
            group by e.jogadorId
            """)
    List<Object[]> agregarPorPeriodo(@Param("de") LocalDate de, @Param("ate") LocalDate ate);

    @Query("select count(distinct e.peladaId) from EstatisticaPartida e where e.dataJogo between :de and :ate")
    long contarPeladas(@Param("de") LocalDate de, @Param("ate") LocalDate ate);

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
            from EstatisticaPartida e
            where e.peladaId <> :peladaId
            group by e.jogadorId
            """)
    List<Object[]> agregarExceto(@Param("peladaId") Long peladaId);
}
