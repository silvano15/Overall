package br.com.overall.pelada;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TimePeladaJogadorRepository extends JpaRepository<TimePeladaJogador, Long> {

    List<TimePeladaJogador> findByTimeIdIn(Collection<Long> timeIds);

    void deleteByTimeIdIn(Collection<Long> timeIds);
}
