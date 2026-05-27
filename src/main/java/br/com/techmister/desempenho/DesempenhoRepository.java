package br.com.techmister.desempenho;

import br.com.techmister.partida.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DesempenhoRepository extends JpaRepository<Desempenho, Long> {

    /** Usado na criação para impedir duplicidade (mesmo jogador, mesma partida). */
    Optional<Desempenho> findByPartidaIdAndJogadorId(Long partidaId, Long jogadorId);

    /** RF-016 CN02: a partida tem desempenhos? */
    boolean existsByPartidaId(Long partidaId);

    /** RF-004 CN02 (parte): o jogador tem desempenhos? */
    boolean existsByJogadorId(Long jogadorId);

    /** Time Ideal: todos os desempenhos das partidas da semana. */
    List<Desempenho> findByPartidaIn(List<Partida> partidas);

    /** Time Ideal: desempenhos de partidas com data dentro da janela informada. */
    List<Desempenho> findByPartidaDataJogoBetween(LocalDate inicio, LocalDate fim);
}
