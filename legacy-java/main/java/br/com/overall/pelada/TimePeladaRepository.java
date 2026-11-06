package br.com.overall.pelada;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimePeladaRepository extends JpaRepository<TimePelada, Long> {

    List<TimePelada> findByPeladaIdOrderByNumeroAsc(Long peladaId);

    void deleteByPeladaId(Long peladaId);
}
