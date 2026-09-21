package br.com.overall.partida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PenaltiRepository extends JpaRepository<Penalti, Long> {

    List<Penalti> findByPartidaIdOrderByOrdemAsc(Long partidaId);

    Optional<Penalti> findFirstByPartidaIdOrderByOrdemDesc(Long partidaId);

    void deleteByPartidaId(Long partidaId);
}
