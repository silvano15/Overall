package br.com.overall.jogador;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Jogador da pelada. Só precisa de nome e foto; 'goleiro' escolhe a fórmula do overall.
 * O campo 'overall' é um cache: ele só muda quando uma pelada é finalizada.
 */
@Entity
@Table(name = "jogador")
public class Jogador {

    public static final int OVERALL_INICIAL = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grupo_id", nullable = false)
    private Long grupoId;

    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    /** Miniatura em data URL (ex.: data:image/jpeg;base64,...). */
    @Column(name = "foto", columnDefinition = "MEDIUMTEXT")
    private String foto;

    @Column(name = "goleiro", nullable = false)
    private boolean goleiro;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "overall", nullable = false)
    private int overall = OVERALL_INICIAL;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Jogador() {}

    public Jogador(Long grupoId, String nome, String foto, boolean goleiro) {
        this.grupoId = grupoId; this.nome = nome; this.foto = foto; this.goleiro = goleiro;
    }

    public Long getId() { return id; }
    public Long getGrupoId() { return grupoId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public boolean isGoleiro() { return goleiro; }
    public void setGoleiro(boolean goleiro) { this.goleiro = goleiro; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public int getOverall() { return overall; }
    public void setOverall(int overall) { this.overall = overall; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
