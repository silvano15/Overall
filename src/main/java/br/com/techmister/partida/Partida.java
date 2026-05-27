package br.com.techmister.partida;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "partida")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "adversario", nullable = false, length = 120)
    private String adversario;

    @Column(name = "data_jogo", nullable = false, unique = true)
    private LocalDate dataJogo;

    @Column(name = "gols_pro")
    private Integer golsPro;

    @Column(name = "gols_contra")
    private Integer golsContra;

    @Column(name = "local_jogo", length = 120)
    private String local;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Partida() {}

    public Long getId() { return id; }
    public String getAdversario() { return adversario; }
    public void setAdversario(String adversario) { this.adversario = adversario; }
    public LocalDate getDataJogo() { return dataJogo; }
    public void setDataJogo(LocalDate dataJogo) { this.dataJogo = dataJogo; }
    public Integer getGolsPro() { return golsPro; }
    public void setGolsPro(Integer golsPro) { this.golsPro = golsPro; }
    public Integer getGolsContra() { return golsContra; }
    public void setGolsContra(Integer golsContra) { this.golsContra = golsContra; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String adversario; private LocalDate dataJogo; private Integer golsPro;
        private Integer golsContra; private String local;
        public Builder adversario(String a) { this.adversario = a; return this; }
        public Builder dataJogo(LocalDate d) { this.dataJogo = d; return this; }
        public Builder golsPro(Integer g) { this.golsPro = g; return this; }
        public Builder golsContra(Integer g) { this.golsContra = g; return this; }
        public Builder local(String l) { this.local = l; return this; }
        public Partida build() {
            Partida p = new Partida(); p.adversario=adversario; p.dataJogo=dataJogo;
            p.golsPro=golsPro; p.golsContra=golsContra; p.local=local; return p;
        }
    }
}
