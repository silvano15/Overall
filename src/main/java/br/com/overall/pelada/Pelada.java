package br.com.overall.pelada;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Um dia de pelada: data/hora, lista de jogadores, times sorteados e partidas. */
@Entity
@Table(name = "pelada")
public class Pelada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_jogo", nullable = false)
    private LocalDate dataJogo;

    @Column(name = "hora_jogo", nullable = false)
    private LocalTime horaJogo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PeladaStatus status = PeladaStatus.ABERTA;

    @Column(name = "jogadores_por_time")
    private Integer jogadoresPorTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_sorteio", length = 20)
    private ModoSorteio modoSorteio;

    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;

    @Column(name = "gols_para_encerrar")
    private Integer golsParaEncerrar;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "finalizada_em")
    private LocalDateTime finalizadaEm;

    public Pelada() {}

    public Pelada(LocalDate dataJogo, LocalTime horaJogo) {
        this.dataJogo = dataJogo; this.horaJogo = horaJogo;
    }

    public Long getId() { return id; }
    public LocalDate getDataJogo() { return dataJogo; }
    public void setDataJogo(LocalDate dataJogo) { this.dataJogo = dataJogo; }
    public LocalTime getHoraJogo() { return horaJogo; }
    public void setHoraJogo(LocalTime horaJogo) { this.horaJogo = horaJogo; }
    public PeladaStatus getStatus() { return status; }
    public void setStatus(PeladaStatus status) { this.status = status; }
    public Integer getJogadoresPorTime() { return jogadoresPorTime; }
    public void setJogadoresPorTime(Integer jogadoresPorTime) { this.jogadoresPorTime = jogadoresPorTime; }
    public ModoSorteio getModoSorteio() { return modoSorteio; }
    public void setModoSorteio(ModoSorteio modoSorteio) { this.modoSorteio = modoSorteio; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public Integer getGolsParaEncerrar() { return golsParaEncerrar; }
    public void setGolsParaEncerrar(Integer golsParaEncerrar) { this.golsParaEncerrar = golsParaEncerrar; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getFinalizadaEm() { return finalizadaEm; }
    public void setFinalizadaEm(LocalDateTime finalizadaEm) { this.finalizadaEm = finalizadaEm; }
}
