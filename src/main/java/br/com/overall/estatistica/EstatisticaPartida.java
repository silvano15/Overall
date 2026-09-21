package br.com.overall.estatistica;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma linha por jogador por partida encerrada. Tudo (overall, rankings, notas) sai daqui. */
@Entity
@Table(name = "estatistica_partida")
public class EstatisticaPartida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partida_id", nullable = false)
    private Long partidaId;

    @Column(name = "pelada_id", nullable = false)
    private Long peladaId;

    @Column(name = "data_jogo", nullable = false)
    private LocalDate dataJogo;

    @Column(name = "jogador_id", nullable = false)
    private Long jogadorId;

    @Column(name = "time_id", nullable = false)
    private Long timeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 10)
    private Resultado resultado;

    @Column(name = "gols", nullable = false)
    private int gols;

    @Column(name = "assistencias", nullable = false)
    private int assistencias;

    @Column(name = "goleiro", nullable = false)
    private boolean goleiro;

    @Column(name = "gols_sofridos", nullable = false)
    private int golsSofridos;

    @Column(name = "sem_sofrer", nullable = false)
    private boolean semSofrer;

    @Column(name = "nota", nullable = false, precision = 4, scale = 2)
    private BigDecimal nota;

    public EstatisticaPartida() {}

    public EstatisticaPartida(Long partidaId, Long peladaId, LocalDate dataJogo, Long jogadorId, Long timeId,
                              Resultado resultado, int gols, int assistencias, boolean goleiro,
                              int golsSofridos, BigDecimal nota) {
        this.partidaId = partidaId; this.peladaId = peladaId; this.dataJogo = dataJogo;
        this.jogadorId = jogadorId; this.timeId = timeId; this.resultado = resultado;
        this.gols = gols; this.assistencias = assistencias; this.goleiro = goleiro;
        this.golsSofridos = golsSofridos; this.semSofrer = goleiro && golsSofridos == 0;
        this.nota = nota;
    }

    public Long getId() { return id; }
    public Long getPartidaId() { return partidaId; }
    public Long getPeladaId() { return peladaId; }
    public LocalDate getDataJogo() { return dataJogo; }
    public Long getJogadorId() { return jogadorId; }
    public Long getTimeId() { return timeId; }
    public Resultado getResultado() { return resultado; }
    public int getGols() { return gols; }
    public int getAssistencias() { return assistencias; }
    public boolean isGoleiro() { return goleiro; }
    public int getGolsSofridos() { return golsSofridos; }
    public boolean isSemSofrer() { return semSofrer; }
    public BigDecimal getNota() { return nota; }
}
