package br.com.techmister.comportamento;

import br.com.techmister.jogador.Jogador;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "comportamento")
public class Comportamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDate dataAvaliacao;

    @Column(name = "nota_foco", nullable = false)
    private Integer notaFoco;

    @Column(name = "nota_disciplina", nullable = false)
    private Integer notaDisciplina;

    @Column(name = "nota_atitude", nullable = false)
    private Integer notaAtitude;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Comportamento() {}

    public Long getId() { return id; }
    public Jogador getJogador() { return jogador; }
    public void setJogador(Jogador jogador) { this.jogador = jogador; }
    public LocalDate getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(LocalDate dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }
    public Integer getNotaFoco() { return notaFoco; }
    public void setNotaFoco(Integer notaFoco) { this.notaFoco = notaFoco; }
    public Integer getNotaDisciplina() { return notaDisciplina; }
    public void setNotaDisciplina(Integer notaDisciplina) { this.notaDisciplina = notaDisciplina; }
    public Integer getNotaAtitude() { return notaAtitude; }
    public void setNotaAtitude(Integer notaAtitude) { this.notaAtitude = notaAtitude; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Jogador jogador; private LocalDate dataAvaliacao;
        private Integer notaFoco; private Integer notaDisciplina; private Integer notaAtitude;
        public Builder jogador(Jogador j) { this.jogador = j; return this; }
        public Builder dataAvaliacao(LocalDate d) { this.dataAvaliacao = d; return this; }
        public Builder notaFoco(Integer n) { this.notaFoco = n; return this; }
        public Builder notaDisciplina(Integer n) { this.notaDisciplina = n; return this; }
        public Builder notaAtitude(Integer n) { this.notaAtitude = n; return this; }
        public Comportamento build() {
            Comportamento c = new Comportamento(); c.jogador=jogador; c.dataAvaliacao=dataAvaliacao;
            c.notaFoco=notaFoco; c.notaDisciplina=notaDisciplina; c.notaAtitude=notaAtitude; return c;
        }
    }
}
