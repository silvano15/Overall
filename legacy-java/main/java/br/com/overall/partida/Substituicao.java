package br.com.overall.partida;

import jakarta.persistence.*;

@Entity
@Table(name = "substituicao")
public class Substituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partida_id", nullable = false)
    private Long partidaId;

    @Column(name = "time_id", nullable = false)
    private Long timeId;

    /** Null = reforço (entrou sem ninguém sair, para completar um time). */
    @Column(name = "saiu_id")
    private Long saiuId;

    @Column(name = "entrou_id", nullable = false)
    private Long entrouId;

    @Column(name = "tempo_ms", nullable = false)
    private long tempoMs;

    public Substituicao() {}

    public Substituicao(Long partidaId, Long timeId, Long saiuId, Long entrouId, long tempoMs) {
        this.partidaId = partidaId; this.timeId = timeId; this.saiuId = saiuId;
        this.entrouId = entrouId; this.tempoMs = tempoMs;
    }

    public Long getId() { return id; }
    public Long getPartidaId() { return partidaId; }
    public Long getTimeId() { return timeId; }
    public Long getSaiuId() { return saiuId; }
    public Long getEntrouId() { return entrouId; }
    public long getTempoMs() { return tempoMs; }
}
