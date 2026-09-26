package br.com.overall.grupo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoPeladaRepository extends JpaRepository<GrupoPelada, Long> {

    List<GrupoPelada> findAllByOrderByNomeAsc();
}
