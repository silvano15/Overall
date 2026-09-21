package br.com.overall.partida;

/** Como a disputa de pênaltis é conduzida. Em todos os modos, empatado o número de cobranças previsto, seguem as alternadas. */
public enum PenaltisModo {
    /** Todos os jogadores batem: cobranças por time = jogadores do maior time em campo. */
    NORMAL,
    /** O ADM escolhe quantas cobranças cada time bate (ex.: 3). Empatou? Vai para as alternadas. */
    PREDETERMINADO,
    /** Direto nas alternadas: uma cobrança de cada time por vez até alguém sair na frente. */
    ALTERNADO
}
