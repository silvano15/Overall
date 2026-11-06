package br.com.overall.grupo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Um grupo de pelada: turma de amigos, galera do trabalho etc. Cada grupo tem seu próprio
 * elenco de jogadores, suas próprias peladas e seu próprio ranking — isolados dos outros grupos.
 */
@Entity
@Table(name = "grupo_pelada")
public class GrupoPelada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    public GrupoPelada() {}

    public GrupoPelada(String nome) {
        this.nome = nome;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
