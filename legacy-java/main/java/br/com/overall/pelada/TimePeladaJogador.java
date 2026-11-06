package br.com.overall.pelada;

import jakarta.persistence.*;

/** Integrante de um time (resultado do sorteio). */
@Entity
@Table(name = "time_pelada_jogador")
public class TimePeladaJogador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time_id", nullable = false)
    private Long timeId;

    @Column(name = "jogador_id", nullable = false)
    private Long jogadorId;

    public TimePeladaJogador() {}

    public TimePeladaJogador(Long timeId, Long jogadorId) {
        this.timeId = timeId; this.jogadorId = jogadorId;
    }

    public Long getId() { return id; }
    public Long getTimeId() { return timeId; }
    public Long getJogadorId() { return jogadorId; }
}
