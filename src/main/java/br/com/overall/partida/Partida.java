package br.com.overall.partida;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Um confronto entre dois times da pelada.
 *
 * <p>Cronômetro: guardamos {@code acumuladoMs} (tempo já jogado) e {@code retomadaEmMs} (quando o relógio
 * voltou a correr, ou null se parado). O tempo decorrido é sempre calculado — assim o cronômetro
 * sobrevive a recarregar a página e continua certo mesmo com o celular bloqueado.
 */
@Entity
@Table(name = "partida")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pelada_id", nullable = false)
    private Long peladaId;

    @Column(name = "ordem", nullable = false)
    private int ordem;

    @Column(name = "time_a_id", nullable = false)
    private Long timeAId;

    @Column(name = "time_b_id", nullable = false)
    private Long timeBId;

    @Column(name = "duracao_segundos", nullable = false)
    private int duracaoSegundos;

    @Column(name = "gols_para_encerrar", nullable = false)
    private int golsParaEncerrar;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PartidaStatus status = PartidaStatus.AGUARDANDO;

    @Column(name = "acumulado_ms", nullable = false)
    private long acumuladoMs;

    @Column(name = "retomada_em_ms")
    private Long retomadaEmMs;

    @Column(name = "vencedor_id")
    private Long vencedorId;

    @Column(name = "vencedor_penaltis_id")
    private Long vencedorPenaltisId;

    @Enumerated(EnumType.STRING)
    @Column(name = "penaltis_modo", length = 20)
    private PenaltisModo penaltisModo;

    /** Cobranças por time antes das alternadas (0 = começa direto nas alternadas). */
    @Column(name = "penaltis_rodadas")
    private Integer penaltisRodadas;

    @Column(name = "encerrada_em")
    private LocalDateTime encerradaEm;

    public Partida() {}

    public Partida(Long peladaId, int ordem, Long timeAId, Long timeBId, int duracaoSegundos, int golsParaEncerrar) {
        this.peladaId = peladaId; this.ordem = ordem; this.timeAId = timeAId; this.timeBId = timeBId;
        this.duracaoSegundos = duracaoSegundos; this.golsParaEncerrar = golsParaEncerrar;
    }

    /** Tempo jogado até {@code agoraMs}, contando o trecho em que o relógio está correndo. */
    public long decorridoMs(long agoraMs) {
        return acumuladoMs + (retomadaEmMs == null ? 0 : Math.max(0, agoraMs - retomadaEmMs));
    }

    public long duracaoMs() { return duracaoSegundos * 1000L; }

    public boolean relogioCorrendo() { return retomadaEmMs != null; }

    public Long getId() { return id; }
    public Long getPeladaId() { return peladaId; }
    public int getOrdem() { return ordem; }
    public Long getTimeAId() { return timeAId; }
    public Long getTimeBId() { return timeBId; }
    public int getDuracaoSegundos() { return duracaoSegundos; }
    public int getGolsParaEncerrar() { return golsParaEncerrar; }
    public PartidaStatus getStatus() { return status; }
    public void setStatus(PartidaStatus status) { this.status = status; }
    public long getAcumuladoMs() { return acumuladoMs; }
    public void setAcumuladoMs(long acumuladoMs) { this.acumuladoMs = acumuladoMs; }
    public Long getRetomadaEmMs() { return retomadaEmMs; }
    public void setRetomadaEmMs(Long retomadaEmMs) { this.retomadaEmMs = retomadaEmMs; }
    public Long getVencedorId() { return vencedorId; }
    public void setVencedorId(Long vencedorId) { this.vencedorId = vencedorId; }
    public Long getVencedorPenaltisId() { return vencedorPenaltisId; }
    public void setVencedorPenaltisId(Long vencedorPenaltisId) { this.vencedorPenaltisId = vencedorPenaltisId; }
    public PenaltisModo getPenaltisModo() { return penaltisModo; }
    public void setPenaltisModo(PenaltisModo penaltisModo) { this.penaltisModo = penaltisModo; }
    public int getPenaltisRodadas() { return penaltisRodadas == null ? 0 : penaltisRodadas; }
    public void setPenaltisRodadas(Integer penaltisRodadas) { this.penaltisRodadas = penaltisRodadas; }
    public LocalDateTime getEncerradaEm() { return encerradaEm; }
    public void setEncerradaEm(LocalDateTime encerradaEm) { this.encerradaEm = encerradaEm; }

    /** Time que segue na quadra: o vencedor no placar ou, no empate, o vencedor dos pênaltis. */
    public Long quemFicaId() { return vencedorId != null ? vencedorId : vencedorPenaltisId; }

    public Long adversarioDe(Long timeId) { return timeAId.equals(timeId) ? timeBId : timeAId; }
}
