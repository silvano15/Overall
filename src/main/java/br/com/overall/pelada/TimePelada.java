package br.com.overall.pelada;

import jakarta.persistence.*;

/** Um dos times sorteados na pelada ("Time 1", "Time 2"...). */
@Entity
@Table(name = "time_pelada")
public class TimePelada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pelada_id", nullable = false)
    private Long peladaId;

    @Column(name = "numero", nullable = false)
    private int numero;

    public TimePelada() {}

    public TimePelada(Long peladaId, int numero) {
        this.peladaId = peladaId; this.numero = numero;
    }

    public Long getId() { return id; }
    public Long getPeladaId() { return peladaId; }
    public int getNumero() { return numero; }
}
