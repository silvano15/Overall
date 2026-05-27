package br.com.techmister.treino;

import br.com.techmister.jogador.Jogador;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "treino")
public class Treino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Column(name = "data_treino", nullable = false)
    private LocalDate dataTreino;

    @Column(name = "acertos", nullable = false)
    private Integer acertos;

    @Column(name = "erros", nullable = false)
    private Integer erros;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Treino() {}

    public Long getId() { return id; }
    public Jogador getJogador() { return jogador; }
    public void setJogador(Jogador jogador) { this.jogador = jogador; }
    public LocalDate getDataTreino() { return dataTreino; }
    public void setDataTreino(LocalDate dataTreino) { this.dataTreino = dataTreino; }
    public Integer getAcertos() { return acertos; }
    public void setAcertos(Integer acertos) { this.acertos = acertos; }
    public Integer getErros() { return erros; }
    public void setErros(Integer erros) { this.erros = erros; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Jogador jogador; private LocalDate dataTreino;
        private Integer acertos; private Integer erros;
        public Builder jogador(Jogador j) { this.jogador = j; return this; }
        public Builder dataTreino(LocalDate d) { this.dataTreino = d; return this; }
        public Builder acertos(Integer a) { this.acertos = a; return this; }
        public Builder erros(Integer e) { this.erros = e; return this; }
        public Treino build() {
            Treino t = new Treino(); t.jogador=jogador; t.dataTreino=dataTreino;
            t.acertos=acertos; t.erros=erros; return t;
        }
    }
}
