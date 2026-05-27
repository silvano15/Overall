package br.com.techmister.desempenho;

import br.com.techmister.jogador.Jogador;
import br.com.techmister.partida.Partida;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "desempenho")
public class Desempenho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Column(name = "nota", nullable = false, precision = 4, scale = 2)
    private BigDecimal nota;

    @Column(name = "gols", nullable = false)
    private Integer gols;

    @Column(name = "assistencias", nullable = false)
    private Integer assistencias;

    @Column(name = "cartoes_amarelos", nullable = false)
    private Integer cartoesAmarelos;

    @Column(name = "cartoes_vermelhos", nullable = false)
    private Integer cartoesVermelhos;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Desempenho() {}

    public Long getId() { return id; }
    public Partida getPartida() { return partida; }
    public void setPartida(Partida partida) { this.partida = partida; }
    public Jogador getJogador() { return jogador; }
    public void setJogador(Jogador jogador) { this.jogador = jogador; }
    public BigDecimal getNota() { return nota; }
    public void setNota(BigDecimal nota) { this.nota = nota; }
    public Integer getGols() { return gols; }
    public void setGols(Integer gols) { this.gols = gols; }
    public Integer getAssistencias() { return assistencias; }
    public void setAssistencias(Integer assistencias) { this.assistencias = assistencias; }
    public Integer getCartoesAmarelos() { return cartoesAmarelos; }
    public void setCartoesAmarelos(Integer cartoesAmarelos) { this.cartoesAmarelos = cartoesAmarelos; }
    public Integer getCartoesVermelhos() { return cartoesVermelhos; }
    public void setCartoesVermelhos(Integer cartoesVermelhos) { this.cartoesVermelhos = cartoesVermelhos; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Partida partida; private Jogador jogador; private BigDecimal nota;
        private Integer gols; private Integer assistencias; private Integer cartoesAmarelos; private Integer cartoesVermelhos;
        public Builder partida(Partida p) { this.partida = p; return this; }
        public Builder jogador(Jogador j) { this.jogador = j; return this; }
        public Builder nota(BigDecimal n) { this.nota = n; return this; }
        public Builder gols(Integer g) { this.gols = g; return this; }
        public Builder assistencias(Integer a) { this.assistencias = a; return this; }
        public Builder cartoesAmarelos(Integer c) { this.cartoesAmarelos = c; return this; }
        public Builder cartoesVermelhos(Integer c) { this.cartoesVermelhos = c; return this; }
        public Desempenho build() {
            Desempenho d = new Desempenho(); d.partida=partida; d.jogador=jogador; d.nota=nota;
            d.gols=gols; d.assistencias=assistencias; d.cartoesAmarelos=cartoesAmarelos; d.cartoesVermelhos=cartoesVermelhos; return d;
        }
    }
}
