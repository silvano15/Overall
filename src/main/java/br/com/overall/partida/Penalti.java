package br.com.overall.partida;

import jakarta.persistence.*;

/** Uma cobrança da disputa de pênaltis. Não conta como gol nas estatísticas do jogador. */
@Entity
@Table(name = "penalti")
public class Penalti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partida_id", nullable = false)
    private Long partidaId;

    @Column(name = "ordem", nullable = false)
    private int ordem;

    @Column(name = "time_id", nullable = false)
    private Long timeId;

    @Column(name = "jogador_id", nullable = false)
    private Long jogadorId;

    @Column(name = "convertido", nullable = false)
    private boolean convertido;

    public Penalti() {}

    public Penalti(Long partidaId, int ordem, Long timeId, Long jogadorId, boolean convertido) {
        this.partidaId = partidaId; this.ordem = ordem; this.timeId = timeId;
        this.jogadorId = jogadorId; this.convertido = convertido;
    }

    public Long getId() { return id; }
    public Long getPartidaId() { return partidaId; }
    public int getOrdem() { return ordem; }
    public Long getTimeId() { return timeId; }
    public Long getJogadorId() { return jogadorId; }
    public boolean isConvertido() { return convertido; }
}
