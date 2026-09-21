package br.com.overall.pelada;

public enum PeladaStatus {
    /** Lista aberta: o ADM cadastra a data/hora e vai adicionando jogadores. */
    ABERTA,
    /** Times sorteados; ainda nenhuma partida criada (pode re-sortear). */
    SORTEADA,
    /** Pelo menos uma partida criada. */
    EM_ANDAMENTO,
    FINALIZADA
}
