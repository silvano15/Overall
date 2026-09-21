package br.com.overall.pelada;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeladaRepository extends JpaRepository<Pelada, Long> {

    /** A pelada "da vez": a não finalizada mais próxima. */
    Optional<Pelada> findFirstByStatusNotOrderByDataJogoAscIdAsc(PeladaStatus status);

    boolean existsByStatusNot(PeladaStatus status);

    List<Pelada> findAllByOrderByDataJogoDescIdDesc();
}
