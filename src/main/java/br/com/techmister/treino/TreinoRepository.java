package br.com.techmister.treino;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TreinoRepository extends JpaRepository<Treino, Long> {

    /** RF-006 CN02: já existe treino do jogador nesta data? */
    Optional<Treino> findByJogadorIdAndDataTreino(Long jogadorId, LocalDate dataTreino);

    /** RF-004 CN02 (parte): há treinos para esse jogador? */
    boolean existsByJogadorId(Long jogadorId);

    /** RF-009 e Time Ideal: treinos de uma janela de datas (semana). */
    List<Treino> findByDataTreinoBetween(LocalDate inicio, LocalDate fim);

    /** Time Ideal: treinos de um jogador específico em uma janela de datas. */
    List<Treino> findByJogadorIdAndDataTreinoBetween(Long jogadorId, LocalDate inicio, LocalDate fim);
}
