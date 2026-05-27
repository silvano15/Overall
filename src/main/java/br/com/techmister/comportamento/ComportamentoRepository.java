package br.com.techmister.comportamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComportamentoRepository extends JpaRepository<Comportamento, Long> {

    /** RF-010 CN02: já existe avaliação desse jogador nesta data? */
    Optional<Comportamento> findByJogadorIdAndDataAvaliacao(Long jogadorId, LocalDate dataAvaliacao);

    /** RF-004 CN02 (parte): há comportamentos para esse jogador? */
    boolean existsByJogadorId(Long jogadorId);

    /** Time Ideal: avaliações de uma janela de datas. */
    List<Comportamento> findByDataAvaliacaoBetween(LocalDate inicio, LocalDate fim);

    /** Time Ideal: avaliações de um jogador específico na semana. */
    List<Comportamento> findByJogadorIdAndDataAvaliacaoBetween(Long jogadorId, LocalDate inicio, LocalDate fim);
}
