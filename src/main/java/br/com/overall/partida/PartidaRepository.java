package br.com.overall.partida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartidaRepository extends JpaRepository<Partida, Long> {

    List<Partida> findByPeladaIdOrderByOrdemAsc(Long peladaId);

    Optional<Partida> findFirstByPeladaIdOrderByOrdemDesc(Long peladaId);

    boolean existsByPeladaIdAndStatusNot(Long peladaId, PartidaStatus status);

    boolean existsByPeladaId(Long peladaId);
}
