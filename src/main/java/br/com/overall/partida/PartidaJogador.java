package br.com.overall.partida;

import jakarta.persistence.*;

/** Quem participou da partida: escalação inicial e quem entrou depois. */
@Entity
@Table(name = "partida_jogador")
public class PartidaJogador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partida_id", nullable = false)
    private Long partidaId;

    @Column(name = "time_id", nullable = false)
    private Long timeId;

    @Column(name = "jogador_id", nullable = false)
    private Long jogadorId;

    @Column(name = "goleiro", nullable = false)
    private boolean goleiro;

    @Column(name = "em_campo", nullable = false)
    private boolean emCampo = true;

    public PartidaJogador() {}

    public PartidaJogador(Long partidaId, Long timeId, Long jogadorId, boolean goleiro) {
        this.partidaId = partidaId; this.timeId = timeId; this.jogadorId = jogadorId; this.goleiro = goleiro;
    }

    public Long getId() { return id; }
    public Long getPartidaId() { return partidaId; }
    public Long getTimeId() { return timeId; }
    public void setTimeId(Long timeId) { this.timeId = timeId; }
    public Long getJogadorId() { return jogadorId; }
    public boolean isGoleiro() { return goleiro; }
    public void setGoleiro(boolean goleiro) { this.goleiro = goleiro; }
    public boolean isEmCampo() { return emCampo; }
    public void setEmCampo(boolean emCampo) { this.emCampo = emCampo; }
}
