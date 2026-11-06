package br.com.overall.partida;

import jakarta.persistence.*;

@Entity
@Table(name = "gol")
public class Gol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partida_id", nullable = false)
    private Long partidaId;

    @Column(name = "time_id", nullable = false)
    private Long timeId;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    /** Null quando o gol não teve assistência (ex.: pênalti). */
    @Column(name = "assistente_id")
    private Long assistenteId;

    @Column(name = "tempo_ms", nullable = false)
    private long tempoMs;

    public Gol() {}

    public Gol(Long partidaId, Long timeId, Long autorId, Long assistenteId, long tempoMs) {
        this.partidaId = partidaId; this.timeId = timeId; this.autorId = autorId;
        this.assistenteId = assistenteId; this.tempoMs = tempoMs;
    }

    public Long getId() { return id; }
    public Long getPartidaId() { return partidaId; }
    public Long getTimeId() { return timeId; }
    public Long getAutorId() { return autorId; }
    public Long getAssistenteId() { return assistenteId; }
    public long getTempoMs() { return tempoMs; }
}
