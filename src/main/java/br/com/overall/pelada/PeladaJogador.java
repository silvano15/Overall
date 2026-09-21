package br.com.overall.pelada;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Jogador na lista de uma pelada. Ao finalizar guarda overall antes/depois e a nota média do dia. */
@Entity
@Table(name = "pelada_jogador")
public class PeladaJogador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pelada_id", nullable = false)
    private Long peladaId;

    @Column(name = "jogador_id", nullable = false)
    private Long jogadorId;

    @Column(name = "overall_antes")
    private Integer overallAntes;

    @Column(name = "overall_depois")
    private Integer overallDepois;

    @Column(name = "nota", precision = 4, scale = 2)
    private BigDecimal nota;

    public PeladaJogador() {}

    public PeladaJogador(Long peladaId, Long jogadorId) {
        this.peladaId = peladaId; this.jogadorId = jogadorId;
    }

    public Long getId() { return id; }
    public Long getPeladaId() { return peladaId; }
    public Long getJogadorId() { return jogadorId; }
    public Integer getOverallAntes() { return overallAntes; }
    public void setOverallAntes(Integer overallAntes) { this.overallAntes = overallAntes; }
    public Integer getOverallDepois() { return overallDepois; }
    public void setOverallDepois(Integer overallDepois) { this.overallDepois = overallDepois; }
    public BigDecimal getNota() { return nota; }
    public void setNota(BigDecimal nota) { this.nota = nota; }
}
