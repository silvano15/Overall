package br.com.overall.jogador;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JogadorRepository extends JpaRepository<Jogador, Long> {

    List<Jogador> findByAtivoTrueOrderByNomeAsc();

    List<Jogador> findByGrupoId(Long grupoId);

    boolean existsByGrupoId(Long grupoId);

    long countByGrupoIdAndAtivoTrue(Long grupoId);
}
