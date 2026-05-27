package br.com.techmister.partida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PartidaRepository extends JpaRepository<Partida, Long> {

    /** RF-014 CN02: conflito de calendário (data já ocupada). */
    Optional<Partida> findByDataJogo(LocalDate dataJogo);

    /** RF-015 CN02: conflito ao tentar mover uma partida para uma data já ocupada por OUTRA partida. */
    Optional<Partida> findByDataJogoAndIdNot(LocalDate dataJogo, Long id);
}
