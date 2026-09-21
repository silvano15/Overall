package br.com.overall.partida.dto;

import br.com.overall.partida.PartidaStatus;
import br.com.overall.partida.PenaltisModo;

import java.util.List;

/**
 * Estado completo de uma partida para a tela ao vivo.
 *
 * <p>Cronômetro: o cliente recebe {@code decorridoMs} e {@code agoraMs} (relógio do servidor no momento da
 * resposta). Se {@code relogioCorrendo}, o tempo atual = decorridoMs + (agora local − momento em que recebeu).
 */
public record PartidaResponse(
        Long id,
        Long peladaId,
        int ordem,
        PartidaStatus status,
        int duracaoSegundos,
        int golsParaEncerrar,
        long decorridoMs,
        boolean relogioCorrendo,
        long agoraMs,
        Lado timeA,
        Lado timeB,
        Long vencedorId,
        Long vencedorPenaltisId,
        List<GolItem> gols,
        List<SubstituicaoItem> substituicoes,
        /** Só presente quando a partida foi (ou está sendo) decidida nos pênaltis. */
        Penaltis penaltis
) {
    public record Lado(Long timeId, int numero, int gols, List<Atleta> jogadores) {}

    public record Atleta(Long jogadorId, boolean goleiro, boolean emCampo) {}

    public record GolItem(Long id, Long timeId, Long autorId, Long assistenteId, long tempoMs) {}

    public record SubstituicaoItem(Long id, Long timeId, Long saiuId, Long entrouId, long tempoMs) {}

    /**
     * @param cobrancasA total de cobranças batidas pelo time A — continua somando mesmo quando o ciclo zera
     * @param rodadas    cobranças previstas por time antes das alternadas
     * @param alternadas true quando a disputa está nas alternadas (desempate, ou modo ALTERNADO)
     * @param cicloA     quem do time A já bateu no ciclo atual (zera quando o time inteiro bate)
     */
    public record Penaltis(int golsA, int golsB, int cobrancasA, int cobrancasB,
                           PenaltisModo modo, int rodadas, boolean alternadas,
                           /** Time que bate a próxima cobrança (null se já decidido). */
                           Long proximoTimeId,
                           /** Time que já venceu a disputa (aguardando o ADM confirmar), ou null. */
                           Long decididoParaTimeId,
                           List<Long> cicloA, List<Long> cicloB,
                           List<Cobranca> cobrancas) {}

    public record Cobranca(int ordem, Long timeId, Long jogadorId, boolean convertido) {}
}
