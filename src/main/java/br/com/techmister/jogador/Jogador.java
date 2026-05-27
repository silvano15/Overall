package br.com.techmister.jogador;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jogador")
public class Jogador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "documento", nullable = false, unique = true, length = 20)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "posicao", nullable = false, length = 30)
    private Posicao posicao;

    @Column(name = "altura_cm")
    private Integer alturaCm;

    @Column(name = "peso_kg", precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime atualizadoEm;

    public Jogador() {}

    public Jogador(Long id, String nome, String documento, Posicao posicao, Integer alturaCm,
                   BigDecimal pesoKg, LocalDate dataNascimento, Boolean ativo,
                   LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id; this.nome = nome; this.documento = documento; this.posicao = posicao;
        this.alturaCm = alturaCm; this.pesoKg = pesoKg; this.dataNascimento = dataNascimento;
        this.ativo = ativo; this.criadoEm = criadoEm; this.atualizadoEm = atualizadoEm;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public Posicao getPosicao() { return posicao; }
    public void setPosicao(Posicao posicao) { this.posicao = posicao; }
    public Integer getAlturaCm() { return alturaCm; }
    public void setAlturaCm(Integer alturaCm) { this.alturaCm = alturaCm; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id; private String nome; private String documento; private Posicao posicao;
        private Integer alturaCm; private BigDecimal pesoKg; private LocalDate dataNascimento;
        private Boolean ativo; private LocalDateTime criadoEm; private LocalDateTime atualizadoEm;
        public Builder id(Long id) { this.id = id; return this; }
        public Builder nome(String nome) { this.nome = nome; return this; }
        public Builder documento(String documento) { this.documento = documento; return this; }
        public Builder posicao(Posicao posicao) { this.posicao = posicao; return this; }
        public Builder alturaCm(Integer alturaCm) { this.alturaCm = alturaCm; return this; }
        public Builder pesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; return this; }
        public Builder dataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; return this; }
        public Builder ativo(Boolean ativo) { this.ativo = ativo; return this; }
        public Jogador build() {
            Jogador j = new Jogador(); j.id=id; j.nome=nome; j.documento=documento; j.posicao=posicao;
            j.alturaCm=alturaCm; j.pesoKg=pesoKg; j.dataNascimento=dataNascimento; j.ativo=ativo; return j;
        }
    }
}
