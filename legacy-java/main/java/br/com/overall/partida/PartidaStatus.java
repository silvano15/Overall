package br.com.overall.partida;

public enum PartidaStatus {
    /** Criada, cronômetro ainda não iniciado (dá para ajustar as escalações). */
    AGUARDANDO,
    EM_ANDAMENTO,
    PAUSADA,
    /** Tempo/gols acabaram empatados: o ADM escolhe entre encerrar ou ir para os pênaltis. */
    EMPATE_PENDENTE,
    PENALTIS,
    ENCERRADA;

    /** Partida "aberta" para eventos de jogo (gols). */
    public boolean emJogo() { return this == EM_ANDAMENTO || this == PAUSADA; }
}
